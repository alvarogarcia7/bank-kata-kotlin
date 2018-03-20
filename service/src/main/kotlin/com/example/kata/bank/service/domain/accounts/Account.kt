package com.example.kata.bank.service.domain.accounts

import arrow.core.Either
import arrow.core.None
import arrow.core.Option
import arrow.core.Some
import com.example.kata.bank.service.domain.AccountRequest
import com.example.kata.bank.service.domain.Id
import com.example.kata.bank.service.domain.Persisted
import com.example.kata.bank.service.domain.transactions.Amount
import com.example.kata.bank.service.domain.transactions.Transaction
import com.example.kata.bank.service.domain.transactions.Transaction.Transfer.*
import com.example.kata.bank.service.domain.transactions.TransactionRepository
import com.example.kata.bank.service.domain.transactions.Tx
import com.example.kata.bank.service.infrastructure.statement.Statement
import com.example.kata.bank.service.infrastructure.statement.StatementLine

interface IAccountService {
    fun requestEmitTransfer(tx: Tx, from: Persisted<Account>, to: Persisted<Account>): Transaction.Transfer
    fun requestReceiveTransfer(tx: Tx, originAccount: Persisted<Account>, destinationAccount: Persisted<Account>): Transaction.Transfer
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

    fun receiveTransfer(tx: Tx, from: Persisted<Account>, to: Persisted<Account>): Received {
        val receivedTransfer = Received(tx, Completed(from.id, to.id))
        transactionRepository.save(createIdentityFor(receivedTransfer))
        return receivedTransfer
    }

    fun emitTransfer(tx: Tx, from: Id, to: Id): Emitted {
        val result = Emitted(tx, Completed(from, to))
        transactionRepository.save(createIdentityFor(result))
        return result
    }

    fun confirmChain(request: Transaction.Transfer.Chain, code: String): Transaction.Transfer {

        val request1 = request.t1 as Transaction.Transfer.Intermediate
        val from = request1.request.from
        val destination = request1.request.destination
        return request.f2.invoke(request.tx, from, destination)
    }

    inline fun <T, S> Option<T>.toEither(left: () -> S): Either<S, T> {
        return when (this) {
            is Some -> Either.right(this.t)
            is None -> Either.left(left.invoke())
        }
    }

    companion object {
        fun transfer(amount: Amount, description: String, from: Persisted<Account>, to: Persisted<Account>): Transaction.Transfer {
            return from.value.requestTransfer(amount, description, from, to)
        }
    }

    private fun requestTransfer(amount: Amount, description: String, from: Persisted<Account>, to: Persisted<Account>): Transaction.Transfer {
        val tx = Tx(amount, clock.getTime(), description)
        val requestEmitTransfer = this.service.requestEmitTransfer(tx, from, to)
        val emitted = requestEmitTransfer
        return if (emitted.blocked()) {
            Chain(tx, emitted) { tx: Tx, from: Persisted<Account>, to: Persisted<Account> ->
                from.value.emitTransfer(tx, from.id, to.id)
                val requestReceiveTransfer = to.value.service.requestReceiveTransfer(tx, from, to)
                requestReceiveTransfer
            }
        } else {
            val requestReceiveTransfer = to.value.service.requestReceiveTransfer(tx, from, to)
            requestReceiveTransfer
        }
    }


}

class IncomingSecurityAccountService(private val accountService: IAccountService, private val security: Security) : IAccountService {
    private val type = this::class.simpleName
    override fun requestEmitTransfer(tx: Tx, from: Persisted<Account>, to: Persisted<Account>): Transaction.Transfer {
        println("GOGOGO!")
        return from.value.emitTransfer(tx, from.id, to.id)
    }

    override fun requestReceiveTransfer(tx: Tx, originAccount: Persisted<Account>, destinationAccount: Persisted<Account>): Transaction.Transfer {
        println("BLOCKING HERE")
        return Transaction.Transfer.Intermediate(tx, Transaction.Transfer.Request.Request(originAccount, destinationAccount, security.generate()))
    }
}

class OutgoingSecurityAccountService(private val accountService: IAccountService, private val security: Security) : IAccountService {
    private val type = this::class.simpleName
    override fun requestEmitTransfer(tx: Tx, from: Persisted<Account>, to: Persisted<Account>): Transaction.Transfer {
        println("BLOCKING HERE")
        return Transaction.Transfer.Intermediate(tx, Transaction.Transfer.Request.Request(from, to, security.generate()))
    }

    override fun requestReceiveTransfer(tx: Tx, from: Persisted<Account>, to: Persisted<Account>): Transaction.Transfer {
        println("GOGOGO!")
        return to.value.receiveTransfer(tx, from, to)
    }
}

class AccountService : IAccountService {
    private val type = this::class.simpleName
    override fun requestEmitTransfer(tx: Tx, from: Persisted<Account>, to: Persisted<Account>): Transaction.Transfer {
        return from.value.emitTransfer(tx, from.id, to.id)
    }

    override fun requestReceiveTransfer(tx: Tx, from: Persisted<Account>, to: Persisted<Account>): Transaction.Transfer {
        return to.value.receiveTransfer(tx, from, to)
    }
}

