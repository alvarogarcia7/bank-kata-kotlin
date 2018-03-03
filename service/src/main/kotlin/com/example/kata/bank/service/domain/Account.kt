package com.example.kata.bank.service.domain

import com.example.kata.bank.service.infrastructure.Statement
import com.example.kata.bank.service.infrastructure.StatementLine
import com.example.kata.bank.service.infrastructure.StatementPrinter

class Account(private val clock: Clock) {
    private val transactionRepository: TransactionRepository = TransactionRepository()

    fun deposit(amount: Amount, description: String) {
        this.transactionRepository.save(Transaction.Deposit(amount, clock.getTime(), description))
    }

    fun withdraw(amount: Amount, description: String) {
        this.transactionRepository.save(Transaction.Withdrawal(amount, clock.getTime(), description))
    }

    fun printStatement(statementPrinter: StatementPrinter) {
        val statementLines = StatementLines.parse(StatementLine.initial(), transactionRepository.findAll())
        val statement = Statement.inReverseOrder(statementLines)
        statementPrinter.print(statement)
    }

    class StatementLines {
        companion object {
            fun parse(initialStatement: StatementLine, transactions: List<Transaction>): List<StatementLine> {
                val initial = Pair(initialStatement, mutableListOf<StatementLine>())
                val (_, statementLines) = transactions.fold(initial,
                        { (previousStatement, result), current ->
                            val currentStatement = StatementLine.parse(current, previousStatement.balance)
                            result.add(currentStatement)
                            Pair(currentStatement, result)
                        })

                return listOf(initialStatement).union(statementLines).toList()
            }
        }
        

    }

}