package com.example.kata.bank.service.domain.accounts

import arrow.core.*
import com.example.kata.bank.service.domain.AccountRequest
import com.example.kata.bank.service.domain.Id
import com.example.kata.bank.service.domain.Persisted
import com.example.kata.bank.service.domain.transactions.Amount
import com.example.kata.bank.service.domain.transactions.Transaction
import com.example.kata.bank.service.domain.transactions.Transaction.Transfer.Emitted
import com.example.kata.bank.service.domain.transactions.Transaction.Transfer.Received
import com.example.kata.bank.service.domain.transactions.TransactionRepository
import com.example.kata.bank.service.domain.transactions.Tx
import com.example.kata.bank.service.infrastructure.statement.Statement
import com.example.kata.bank.service.infrastructure.statement.StatementLine

interface IAccountService {
    val securityProvider: Option<Security>
    val receivingSecurityProvider: Option<Security>

    //    fun confirmOperation(transfer: Transaction.Transfer.Incoming.Request): Transaction.Transfer.Incoming.Received
//    fun requestEmitTransfer(tx: Tx, from: Persisted<Account>, to: Persisted<Account>): Transaction.Transfer

    fun transfer(tx: Tx, from: Persisted<Account>, to: Persisted<Account>): Transaction.Transfer
}

class Account(
        val clock: Clock,
        val name: String,
        val type: AccountType = AccountType.Personal,
        private val service: IAccountService = AccountService()
) {

    private val transactionRepository: TransactionRepository = TransactionRepository()

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
        val transaction = this.createIdentityFor(Transaction.Cost(Tx(cost, this.clock.getTime(), description)))
        this.transactionRepository.save(transaction)
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

    fun receiveTransfer(
            transfer: Transaction.Transfer.Emitted,
            originAccount: Persisted<Account>,
            destinationAccount: Persisted<Account>
    ): Received {
        val receivedTransfer = Received(transfer.tx, Transaction.Transfer.Completed(originAccount.id, destinationAccount.id))
        val persistedTransfer = createIdentityFor(receivedTransfer)
        transactionRepository.save(persistedTransfer)
        return receivedTransfer
    }

    fun requestEmitTransfer(tx: Tx, from: Persisted<Account>, to: Persisted<Account>): Transaction.Transfer {
        return service.securityProvider.map {
            Transaction.Transfer.Intermediate(tx, Transaction.Transfer.Request(from, to, it.generate()))
        }.getOrElse {
            Transaction.Transfer.Emitted(tx, Transaction.Transfer.Completed(from.id, to.id))
        }.let {
            val persistedTransfer = createIdentityFor(it)
            transactionRepository.save(persistedTransfer)
            it
        }
    }

    fun emitTransfer(tx: Tx, from: Id, to: Id): Emitted {
        val result = Emitted(tx, Transaction.Transfer.Completed(from, to))
        val persistedTransfer = createIdentityFor(result)
        transactionRepository.save(persistedTransfer)
        return result
    }

    fun requestReceiveTransfer(
            request: Transaction.Transfer.Emitted,
            originAccount: Persisted<Account>,
            destinationAccount: Persisted<Account>
    ): Transaction.Transfer {
        val transfer = when (service.receivingSecurityProvider) {
            is Some -> {
                Transaction.Transfer.Intermediate(
                        request.tx,
                        Transaction.Transfer.Request(
                                originAccount,
                                destinationAccount,
                                (this.service.receivingSecurityProvider as Some<Security>).t.generate()
                        )
                )
            }
            is None -> {
                val transfer = Transaction.Transfer.Received(request.tx, Transaction.Transfer.Completed(
                        originAccount.id,
                        destinationAccount.id
                ))
                val persistedTransfer = createIdentityFor(transfer)
                transactionRepository.save(persistedTransfer)
                transfer
            }
        }
        return transfer
    }

    private fun confirmIngoingRequestOperation(request: Transaction.Transfer.Intermediate): Received {
        val transfer = Received(request.tx, Transaction.Transfer.Completed(request.request.from.id, request.request.destination.id))
        val persistedTransfer = createIdentityFor(transfer)
        transactionRepository.save(persistedTransfer)
        return transfer
    }

    fun confirmOutgoingRequestOperation(request: Transaction.Transfer.Intermediate): Emitted {
        val transfer = Emitted(request.tx, Transaction.Transfer.Completed(request.request.from.id, request.request.destination.id))
        val persistedTransfer = createIdentityFor(transfer)
        transactionRepository.save(persistedTransfer)
        return transfer
    }

    inline fun <T, S> Option<T>.toEither(left: () -> S): Either<S, T> {
        return when (this) {
            is Some -> Either.right(this.t)
            is None -> Either.left(left.invoke())
        }
    }

    companion object {
        fun transfer(amount: Amount, description: String, from: Persisted<Account>, to: Persisted<Account>): Transaction.Transfer {
            return from.value.transfer(amount, description, from, to)
        }

        fun confirmOperation(request: Transaction.Transfer.Intermediate): Transaction.Transfer {
            request.request.destination.value.confirmIngoingRequestOperation(request)
            return request.request.from.value.confirmOutgoingRequestOperation(request)
        }
    }

    private fun transfer(amount: Amount, description: String, from: Persisted<Account>, to: Persisted<Account>): Transaction.Transfer {
        val tx = Tx(amount, clock.getTime(), description)
        return service.transfer(tx, from, to)
    }

}

class IncomingSecurityAccountService(accountService: IAccountService, receivingSecurity: Option<Security>) : IAccountService {
    override fun transfer(tx: Tx, from: Persisted<Account>, to: Persisted<Account>): Transaction.Transfer {
        return Transaction.Transfer.Intermediate(tx, Transaction.Transfer.Request(from, to, receivingSecurityProvider.map { it.generate() }.get()))
    }

    override val securityProvider: Option<Security> = accountService.securityProvider
    override val receivingSecurityProvider: Option<Security> = receivingSecurity
}

class AccountService : IAccountService {
    override fun transfer(tx: Tx, from: Persisted<Account>, to: Persisted<Account>): Transaction.Transfer {
        val emitted = from.value.emitTransfer(tx, from.id, to.id)
        return to.value.receiveTransfer(emitted, from, to)
    }

    fun confirmOperation(transfer: Transaction.Transfer.Intermediate): Transaction.Transfer {
        val emittedTransfer = transfer.request.from.value.emitTransfer(Tx(transfer.tx.amount, transfer.request.from.value.clock.getTime(), transfer.tx.description), transfer.request.from.id, transfer.request.destination.id)
        return transfer.request.destination.value.requestReceiveTransfer(emittedTransfer, transfer.request.from, transfer.request.destination)
    }

//    override fun confirmOperation(transfer: Transaction.Transfer.Incoming.Request): Transaction.Transfer.Incoming.Received {
//        val origin = transfer.request.from.value
//        val emitted = origin.emitTransfer(transfer.tx, transfer.request.from.id, transfer.request.destination.id)
//
//        val destination = transfer.request.destination.value
//        destination.receiveTransfer(emitted, transfer.request.from, transfer.request.destination)
//        val confirmOutgoingRequestOperation = destination.requestReceiveTransfer(transfer)
//        return confirmOutgoingRequestOperation
//    }

    override val securityProvider: Option<Security> = None
    override val receivingSecurityProvider: Option<Security> = None

}

