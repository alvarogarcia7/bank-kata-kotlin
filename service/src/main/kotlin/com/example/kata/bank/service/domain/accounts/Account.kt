package com.example.kata.bank.service.domain.accounts

import arrow.core.*
import com.example.kata.bank.service.domain.AccountRequest
import com.example.kata.bank.service.domain.Id
import com.example.kata.bank.service.domain.Persisted
import com.example.kata.bank.service.domain.transactions.Amount
import com.example.kata.bank.service.domain.transactions.Transaction
import com.example.kata.bank.service.domain.transactions.Transaction.Transfer.Incoming.Received
import com.example.kata.bank.service.domain.transactions.Transaction.Transfer.Outgoing.Emitted
import com.example.kata.bank.service.domain.transactions.TransactionRepository
import com.example.kata.bank.service.domain.transactions.Tx
import com.example.kata.bank.service.infrastructure.statement.Statement
import com.example.kata.bank.service.infrastructure.statement.StatementLine

class Account(
        private val clock: Clock,
        val name: String,
        val type: AccountType = AccountType.Personal,
        private val securityProvider: Option<Security> = None,
        private val receivingSecurityProvider: Option<Security> = None
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

    companion object {
        fun transfer(operationAmount: Amount, description: String, originAccount: Persisted<Account>, destinationAccount: Persisted<Account>): Either<List<Exception>,
                Transaction.Transfer> {
            val request = originAccount.value.requestEmitTransfer(operationAmount, description, originAccount, destinationAccount)
            return when (request) {
                is Emitted -> {
                    if (destinationAccount.value.receivingSecurityProvider.isDefined()) {
                        destinationAccount.value.receiveIncomingTransfer(operationAmount, description, originAccount, destinationAccount)
                    } else {
                        destinationAccount.value.receiveEmittedTransfer(operationAmount, description, originAccount.id, destinationAccount.id)
                        Either.right(Received(Tx(operationAmount, request.tx.time, description), Transaction.Transfer.Completed(originAccount.id, destinationAccount.id)))
                    }
                }
                is Transaction.Transfer.Outgoing.Request -> {
                    Either.right(request)
                }
            }
        }

        fun confirmOperation(transfer: Transaction.Transfer.Outgoing.Request): Any {
            transfer.request.from.value.emitTransfer(transfer.tx.amount, transfer.tx.description, transfer.request.from.id, transfer.request.destination.id)
            return transfer.request.destination.value.receiveEmittedTransfer(transfer.tx.amount, transfer.tx.description, transfer.request.from.id, transfer.request.destination.id)
        }
    }

    private fun requestEmitTransfer(operationAmount: Amount, description: String, from: Persisted<Account>, to: Persisted<Account>): Transaction.Transfer.Outgoing {
        return securityProvider.map {
            Transaction.Transfer.Outgoing.Request(Tx(operationAmount, clock.getTime(), description), Transaction.Transfer.Request(from, to, it.generate()))
        }.getOrElse {
            Emitted(Tx(operationAmount, clock.getTime(), description), Transaction.Transfer.Completed(from.id, to.id))
        }.let {
            val persistedTransfer = createIdentityFor(it)
            transactionRepository.save(persistedTransfer)
            it
        }
    }

    private fun emitTransfer(amount: Amount, description: String, from: Id, to: Id) {
        val x = Emitted(Tx(amount, clock.getTime(), description), Transaction.Transfer.Completed(from, to))
        val persistedTransfer = createIdentityFor(x)
        transactionRepository.save(persistedTransfer)
    }

    private fun receiveIncomingTransfer(operationAmount: Amount, description: String, from: Persisted<Account>, destination: Persisted<Account>):
            Either<List<Exception>, Transaction.Transfer.Incoming.Request> {
        val transfer = Transaction.Transfer.Incoming.Request(Tx(operationAmount, clock.getTime(), description), Transaction.Transfer.Request(from, destination, this
                .receivingSecurityProvider.map {
            it
                    .generate()
        }.get()))
        val persistedTransfer = createIdentityFor(transfer)
        transactionRepository.save(persistedTransfer)
        return Either.right(transfer)
    }

    private fun receiveEmittedTransfer(operationAmount: Amount, description: String, from: Id, destinationId: Id): Either<List<Exception>, Received> {
        val transfer = Received(Tx(operationAmount, clock.getTime(), description), Transaction.Transfer.Completed(from, destinationId))
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