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
        statementPrinter.print(Statement.including(StatementLine.initial(), transactionRepository.findAll()))
    }

}