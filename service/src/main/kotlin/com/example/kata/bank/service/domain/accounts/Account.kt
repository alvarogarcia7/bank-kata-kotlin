package com.example.kata.bank.service.domain.accounts

import arrow.core.Either
import arrow.core.Option
import arrow.core.flatMap
import com.example.kata.bank.service.domain.AccountRequest
import com.example.kata.bank.service.domain.Id
import com.example.kata.bank.service.domain.Persisted
import com.example.kata.bank.service.domain.transactions.Amount
import com.example.kata.bank.service.domain.transactions.Transaction
import com.example.kata.bank.service.domain.transactions.TransactionRepository
import com.example.kata.bank.service.infrastructure.statement.Statement
import com.example.kata.bank.service.infrastructure.statement.StatementLine

class Account(private val clock: Clock, val name: String, val type: AccountType = AccountType.Personal) {
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

    private fun createIdentityFor(transaction: Transaction): Persisted<Transaction> {
        val id = Id.random()
        return Persisted.`for`(transaction, id)
    }

    private fun createIdentityFor(transaction: Transaction.TransferEmitted): Persisted<Transaction.TransferEmitted> {
        val id = Id.random()
        return Persisted.`for`(transaction, id)
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
            return originAccount.value.emitTransfer(operationAmount, description, destinationAccount.id).flatMap {
                destinationAccount.value.receiveTransfer(operationAmount, description, originAccount.id)
                Either.right(Transaction.Transfer(operationAmount, it.time, description, originAccount.id, destinationAccount.id))
            }
        }
    }

    private fun receiveTransfer(operationAmount: Amount, description: String, from: Id): Either<List<Exception>, Transaction.TransferReceived> {
        val transfer = Transaction.TransferReceived(operationAmount, clock.getTime(), description, from)
        val persistedTransfer = createIdentityFor(transfer)
        transactionRepository.save(persistedTransfer)
        return Either.right(transfer)
    }

    private fun emitTransfer(operationAmount: Amount, description: String, to: Id): Either<List<Exception>, Transaction.TransferEmitted> {
        val transfer = Transaction.TransferEmitted(operationAmount, clock.getTime(), description, to)
        val persistedTransfer = createIdentityFor(transfer)
        transactionRepository.save(persistedTransfer)
        return Either.right(transfer)
    }

}