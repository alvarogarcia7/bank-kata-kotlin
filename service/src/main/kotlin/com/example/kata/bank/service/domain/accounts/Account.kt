package com.example.kata.bank.service.domain.accounts

import arrow.core.Either
import arrow.core.None
import arrow.core.Option
import arrow.core.Some
import com.example.kata.bank.service.FinalState
import com.example.kata.bank.service.State
import com.example.kata.bank.service.domain.AccountRequest
import com.example.kata.bank.service.domain.Id
import com.example.kata.bank.service.domain.Persisted
import com.example.kata.bank.service.domain.transactions.Amount
import com.example.kata.bank.service.domain.transactions.Transaction
import com.example.kata.bank.service.domain.transactions.Transaction.Transfer
import com.example.kata.bank.service.domain.transactions.Transaction.Transfer.TransferRequest
import com.example.kata.bank.service.domain.transactions.Tx
import com.example.kata.bank.service.infrastructure.statement.Statement
import com.example.kata.bank.service.infrastructure.statement.StatementLine
import com.example.kata.bank.service.infrastructure.storage.InMemorySimpleRepository
import com.example.kata.bank.service.infrastructure.transactions.TransactionSimpleRepository

class Account(
        val clock: Clock,
        val name: String,
        val type: AccountType = AccountType.Personal,
        private val incomingSecurity: Option<Security> = None,
        private val outgoingSecurity: Option<Security> = None,
        val number: Number = Number.of(Id.random().value)
) : OutgoingTransfer, IncomingTransfer {


    private val transactionRepository: InMemorySimpleRepository<Transaction> = TransactionSimpleRepository()
    private val pendingTransfers: MutableMap<Id, Pair<State<Transfer.TransferRequest>, Tx>> = mutableMapOf()


    fun pendingTransfers(): Map<Id, State<Transfer.TransferRequest>> {
        return pendingTransfers
                .mapValues { a -> a.value.first }
    }

    fun deposit(amount: Amount, description: String): Id {
        val transaction = createIdentityFor(Transaction.Deposit(Tx(amount, clock.getTime(), description)))
        this.transactionRepository.save(transaction)
        return transaction.id
    }

    @Synchronized
    fun withdraw(operationAmount: Amount, description: String): Either<List<Exception>, Id> {
        if (type == AccountType.Personal) {
            if (operationAmount.greaterThan(balance())) {
                return Either.left(listOf(Exception("Cannot go overdraft")))
            }
        }
        val transaction = createIdentityFor(Transaction.Withdrawal(Tx(operationAmount, clock.getTime(), description)))
        this.transactionRepository.save(transaction)
        return Either.right(transaction.id)
    }

    private fun <T> createIdentityFor(value: T): Persisted<T> {
        return Persisted.`for`(value, Id.random())
    }

    fun createStatement(statementRequest: AccountRequest.StatementRequest): Statement {
        type.determineStatementCost(transactionRepository.findAll().map { it.value }, statementRequest)
                .map { (amount, description) ->
                    this.addCost(amount, description)
                }
        val transactions = transactionRepository.findAll()
                .map { it.value }
                .filter(statementRequest.filter)
        val statementLines = StatementLines.parse(StatementLine.initial(), transactions)

        return Statement.inReverseOrder(statementLines)
    }

    private fun addCost(cost: Amount, description: String) {
        this.transactionRepository.save(createIdentityFor(Transaction.Cost(Tx(cost, clock.getTime(), description))))
    }

    fun balance(): Amount {
        val transactions = this.findAll()
        val result = transactions.map { it.value }
                .foldRight(Amount.of("0"), { transaction, acc -> transaction.subtotal(acc) })
        return result
    }

    val find = transactionRepository::findBy

    val findAll = transactionRepository::findAll

    class StatementLines {
        companion object {
            fun parse(initialStatement: StatementLine, transactions: List<Transaction>): List<StatementLine> {
                val initial = mutableListOf(initialStatement)
                val statementLines = transactions.fold(initial,
                        { result, current ->
                            val currentStatement = StatementLine.parse(current, result.last().balance)
                            result.add(currentStatement)
                            result
                        })
                return statementLines.toList()
            }


        }

    }

    enum class AccountType {
        Personal {
            override fun determineStatementCost(map: List<Transaction>, statementRequest: AccountRequest.StatementRequest): Option<Pair<Amount, String>> {
                if (map.filter(statementRequest.filter).isNotEmpty()) {
                    return Option.pure(Pair(Amount.Companion.of("1"), "Statement creation"))
                }
                return Option.empty()
            }
        },
        Premium {
            override fun determineStatementCost(map: List<Transaction>, statementRequest: AccountRequest.StatementRequest): Option<Pair<Amount, String>> {
                return Option.empty()
            }

        };

        abstract fun determineStatementCost(map: List<Transaction>, statementRequest: AccountRequest.StatementRequest): Option<Pair<Amount, String>>
    }

    companion object {
        fun transfer(amount: Amount, description: String, from: Persisted<Account>, to: Persisted<Account>) {
            TransferDiagram.Initial(TransferRequest(from.value, to.value, from.value.genTx(amount, description))).transition()
        }
    }


    private fun genTx(amount: Amount, description: String) = Tx(amount, clock.getTime(), description)

    override fun confirmIncoming(transferId: Id) {
        val tx = this.pendingTransfers[transferId]!!.second
        transactionRepository.save(createIdentityFor(Transaction.IncomingCompletedTransfer(tx)))
    }

    override fun confirmOutgoing(transferId: Id) {
        val state = this.pendingTransfers[transferId]!!
        this.transactionRepository.save(createIdentityFor(Transaction.OutgoingCompletedTransfer(state.second)))
    }

    override fun userConfirmIncoming(transferId: Id) {
        this.pendingTransfers[transferId]!!.first.transition()
        this.pendingTransfers.remove(transferId)
    }

    override fun userConfirmOutgoing(transferId: Id) {
        userConfirmIncoming(transferId)
    }

    override fun requestIncomingPayload(request: TransferRequest): Transfer {
        return generateTransferPayload(incomingSecurity, request)
    }

    override fun requestOutgoingPayload(request: TransferRequest): Transfer {
        return generateTransferPayload(outgoingSecurity, request)
    }

    private fun generateTransferPayload(security: Option<Security>, transferRequest: TransferRequest): Transfer {
        return when (security) {
            is Some -> {
                val request = Transfer(transferRequest.request, TransferPayload.SecureTransferPayload(Id.random(), security.t.generate(), transferRequest))
                val persisted = createIdentityFor(request)
                transactionRepository.save(persisted)
                request
            }
            is None -> {
                val request = Transfer(transferRequest.request, TransferPayload.NotSecureTransferPayload(Id.random(), transferRequest))
                val persisted = createIdentityFor(request)
                transactionRepository.save(persisted)
                request
            }
        }
    }

    override fun register(transferId: Id, diagram: State<TransferRequest>, tx: Tx) {
        this.pendingTransfers[transferId] = Pair(diagram, tx)
    }

    data class Number private constructor(val value: String) {
        companion object {
            fun of(value: String): Number {
                return Number(value)
            }
        }
    }
}

interface IncomingTransfer {
    fun confirmIncoming(transferId: Id)
    fun userConfirmIncoming(transferId: Id)
    fun requestIncomingPayload(request: Transfer.TransferRequest): Transfer
    fun register(transferId: Id, diagram: State<TransferRequest>, tx: Tx)
}

interface OutgoingTransfer {
    fun confirmOutgoing(transferId: Id)
    fun userConfirmOutgoing(transferId: Id)
    fun requestOutgoingPayload(request: Transfer.TransferRequest): Transfer
    fun register(transferId: Id, diagram: State<TransferRequest>, tx: Tx)
}

sealed class TransferPayload {
    abstract val transferId: Id
    abstract val request: TransferRequest

    data class SecureTransferPayload(override val transferId: Id, val code: String, override val request: TransferRequest) : TransferPayload()
    data class NotSecureTransferPayload(override val transferId: Id, override val request: TransferRequest) : TransferPayload()
}

sealed class TransferDiagram : State<TransferRequest> {

    data class Initial(val transferRequest: TransferRequest) : State<TransferRequest> {
        override fun transition(): State<TransferRequest> {
            val outgoingPayload = transferRequest.from.requestOutgoingPayload(transferRequest)
            val incomingTransferRequest = IncomingTransferRequest(outgoingPayload.payload.transferId, transferRequest)
            return when (outgoingPayload.payload) {
                is TransferPayload.SecureTransferPayload -> {
                    val newState = WaitingForOutgoingConfirmation(incomingTransferRequest)
                    transferRequest.from.register(outgoingPayload.payload.transferId, newState, outgoingPayload.tx)
                    newState
                }
                is TransferPayload.NotSecureTransferPayload -> {
                    val newState = IncomingRequest(incomingTransferRequest)
                    transferRequest.from.register(outgoingPayload.payload.transferId, newState, outgoingPayload.tx)
                    newState.transition()
                    newState
                }
            }
        }
    }


    data class IncomingTransferRequest(val incomingTransferId: Id, val TransferRequest: TransferRequest)
    data class CompleteTransferRequest(val outgoingTransferId: Id, val incomingTransferRequest: IncomingTransferRequest)


    data class WaitingForOutgoingConfirmation(val transferRequest: IncomingTransferRequest) : State<TransferRequest> {
        override fun transition(): State<TransferRequest> {
            return IncomingRequest(transferRequest).transition()
        }
    }

    data class IncomingRequest(val transferRequest: IncomingTransferRequest) : State<TransferRequest> {
        override fun transition(): State<TransferRequest> {
            val to = transferRequest.TransferRequest.to
            val payload = to.requestIncomingPayload(transferRequest.TransferRequest)
            val transferRequest1 = CompleteTransferRequest(payload.payload.transferId, transferRequest)
            return when (payload.payload) {
                is TransferPayload.SecureTransferPayload -> {
                    val newState = WaitingForIncomingConfirmation(transferRequest1)
                    to.register(payload.payload.transferId, newState, payload.tx)
                    newState
                }
                is TransferPayload.NotSecureTransferPayload -> {
                    val newState = PerformingActions(transferRequest1)
                    to.register(payload.payload.transferId, newState, payload.tx)
                    newState.transition()
                }
            }
        }
    }

    data class WaitingForIncomingConfirmation(val transferRequest: CompleteTransferRequest) : State<TransferRequest> {
        override fun transition(): State<TransferRequest> {
            return PerformingActions(transferRequest).transition()
        }
    }

    data class PerformingActions(private val transferRequest: CompleteTransferRequest) : State<TransferRequest> {
        override fun transition(): State<TransferRequest> {
            val trRequest = transferRequest.incomingTransferRequest.TransferRequest
            trRequest.from.confirmOutgoing(transferRequest.incomingTransferRequest.incomingTransferId)
            trRequest.to.confirmIncoming(transferRequest.outgoingTransferId)
            return Confirmed(transferRequest)
        }
    }

    data class Confirmed(val transferRequest: CompleteTransferRequest) : FinalState<TransferRequest>()
}
