package com.example.kata.bank.service.domain.accounts

import arrow.core.Either
import arrow.core.None
import arrow.core.Option
import arrow.core.Some
import com.example.kata.bank.service.State
import com.example.kata.bank.service.domain.AccountRequest
import com.example.kata.bank.service.domain.Id
import com.example.kata.bank.service.domain.Persisted
import com.example.kata.bank.service.domain.transactions.Amount
import com.example.kata.bank.service.domain.transactions.Transaction
import com.example.kata.bank.service.domain.transactions.Transaction.Transfer
import com.example.kata.bank.service.domain.transactions.Transaction.Transfer.TransferRequest
import com.example.kata.bank.service.domain.transactions.Tx
import com.example.kata.bank.service.domain.transfers.IncomingTransfer
import com.example.kata.bank.service.domain.transfers.OutgoingTransfer
import com.example.kata.bank.service.domain.transfers.TransferDiagram
import com.example.kata.bank.service.domain.transfers.TransferPayload
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

