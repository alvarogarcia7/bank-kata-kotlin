package com.example.kata.bank.service.domain.accounts

import com.example.kata.bank.service.domain.Id
import com.example.kata.bank.service.domain.Persisted
import com.example.kata.bank.service.domain.transactions.Amount
import com.example.kata.bank.service.domain.transactions.Transaction
import com.example.kata.bank.service.domain.transactions.TransactionRepository
import com.example.kata.bank.service.infrastructure.statement.Statement
import com.example.kata.bank.service.infrastructure.statement.StatementLine
import com.example.kata.bank.service.infrastructure.statement.StatementPrinter
import com.fasterxml.jackson.annotation.ObjectIdGenerators

class Account(private val clock: Clock, val name: String) {
    private val transactionRepository: TransactionRepository = TransactionRepository()

    fun deposit(amount: Amount, description: String): Id {
        val transaction = createIdentityFor(Transaction.Deposit(amount, clock.getTime(), description))
        this.transactionRepository.save(transaction)
        return transaction.id
    }

    fun withdraw(amount: Amount, description: String): Id {
        val transaction = createIdentityFor(Transaction.Withdrawal(amount, clock.getTime(), description))
        this.transactionRepository.save(transaction)
        return transaction.id
    }

    private fun createIdentityFor(transaction: Transaction): Persisted<Transaction> {
        val id = Id.of(ObjectIdGenerators.UUIDGenerator().generateId(transaction).toString())
        return Persisted.`for`(transaction, id)
    }

    fun printStatement(statementPrinter: StatementPrinter) {
        val statementLines = StatementLines.parse(StatementLine.initial(), transactionRepository.findAll().map { it.value })
        val statement = Statement.inReverseOrder(statementLines)
        statementPrinter.print(statement)
    }

    fun createStatement(): Statement {
        this.withdraw(Amount.Companion.of("1"), "Statement creation")
        val statementLines = StatementLines.parse(StatementLine.initial(), transactionRepository.findAll().map { it.value })
        return Statement.inReverseOrder(statementLines)
    }

    fun balance(): Amount {
        val transactions = this.findAll()
        val result = transactions.map { it.value }
                .foldRight(Amount.of("0"), { ele, acc ->

                    when (ele) {
                        is Transaction.Withdrawal -> {
                            acc.subtract(ele.amount)
                        }
                        is Transaction.Deposit -> {
                            acc.add(ele.amount)
                        }
                        is Transaction.Cost -> {
                            acc.subtract(ele.amount)
                        }
                    }
                })
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

}