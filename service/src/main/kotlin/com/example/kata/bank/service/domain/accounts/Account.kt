package com.example.kata.bank.service.domain.accounts

import arrow.core.*
import com.example.kata.bank.service.domain.AccountRequest
import com.example.kata.bank.service.domain.Id
import com.example.kata.bank.service.domain.Persisted
import com.example.kata.bank.service.domain.transactions.Amount
import com.example.kata.bank.service.domain.transactions.Transaction
import com.example.kata.bank.service.domain.transactions.TransactionRepository
import com.example.kata.bank.service.infrastructure.statement.Statement
import com.example.kata.bank.service.infrastructure.statement.StatementLine

class Account(private val clock: Clock, val name: String, val type: AccountType = AccountType.Personal, private val securityProvider: Option<Security> = None) {
    private val transactionRepository: TransactionRepository = TransactionRepository()

    fun deposit(amount: Amount, description: String): Id {
        val transaction = createIdentityFor(Transaction.Deposit(amount, clock.getTime(), description))
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
        val transaction = createIdentityFor(Transaction.Withdrawal(operationAmount, clock.getTime(), description))
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
        val transaction = this.createIdentityFor(Transaction.Cost(cost, this.clock.getTime(), description))
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

    companion object {
        fun transfer(operationAmount: Amount, description: String, originAccount: Persisted<Account>, destinationAccount: Persisted<Account>): Either<List<Exception>,
                Transaction.Transfer> {
            val request = originAccount.value.requestEmitTransfer(operationAmount, description, destinationAccount.id)
            return when (request) {
                is Transaction.Transfer.Outgoing.Emitted -> {
                    destinationAccount.value.receiveTransfer(operationAmount, description, originAccount.id)
                    Either.right(Transaction.Transfer.Completed(operationAmount, request.time, description, originAccount.id, destinationAccount.id))
                }
                is Transaction.Transfer.Outgoing.Request -> {
                    Either.right(request)
                }
            }
        }
    }

    private fun requestEmitTransfer(operationAmount: Amount, description: String, to: Id): Transaction.Transfer.Outgoing {
        return securityProvider.map {
            Transaction.Transfer.Outgoing.Request(operationAmount, clock.getTime(), description, to, it.generate())
        }.getOrElse {
            Transaction.Transfer.Outgoing.Emitted(operationAmount, clock.getTime(), description, to)
        }.let {
            val persistedTransfer = createIdentityFor(it)
            transactionRepository.save(persistedTransfer)
            it
        }
    }

    private fun receiveTransfer(operationAmount: Amount, description: String, from: Id): Either<List<Exception>, Transaction.Transfer.TransferReceived> {
        val transfer = Transaction.Transfer.TransferReceived(operationAmount, clock.getTime(), description, from)
        val persistedTransfer = createIdentityFor(transfer)
        transactionRepository.save(persistedTransfer)
        return Either.right(transfer)
    }

    inline fun <T, S> Option<T>.toEither(left: () -> S): Either<S, T> {
        return when (this) {
            is Some -> Either.right(this.t)
            is None -> Either.left(left.invoke())
        }
    }
}