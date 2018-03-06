package com.example.kata.bank.service.domain

import com.example.kata.bank.service.infrastructure.statement.Statement
import com.example.kata.bank.service.infrastructure.statement.StatementLine
import com.example.kata.bank.service.infrastructure.statement.StatementPrinter
import com.fasterxml.jackson.annotation.ObjectIdGenerators
import java.util.*

class Account(private val clock: Clock) {
    private val transactionRepository: TransactionRepository = TransactionRepository()
    val name = "name"

    fun deposit(amount: Amount, description: String): UUID {
        val transaction = createIdentityFor(Transaction.Deposit(amount, clock.getTime(), description))
        this.transactionRepository.save(transaction)
        return transaction.id
    }

    fun withdraw(amount: Amount, description: String): UUID {
        val transaction = createIdentityFor(Transaction.Withdrawal(amount, clock.getTime(), description))
        this.transactionRepository.save(transaction)
        return transaction.id
    }

    private fun createIdentityFor(transaction: Transaction): Persisted<Transaction> {
        val id = ObjectIdGenerators.UUIDGenerator().generateId(transaction)
        return Persisted.`for`(transaction, id)
    }

    fun printStatement(statementPrinter: StatementPrinter) {
        val statementLines = StatementLines.parse(StatementLine.initial(), transactionRepository.findAll().map { it.value })
        val statement = Statement.inReverseOrder(statementLines)
        statementPrinter.print(statement)
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

}