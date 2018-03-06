package com.example.kata.bank.service.domain

import com.example.kata.bank.service.infrastructure.statement.Statement
import com.example.kata.bank.service.infrastructure.statement.StatementLine
import com.example.kata.bank.service.infrastructure.statement.StatementPrinter

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