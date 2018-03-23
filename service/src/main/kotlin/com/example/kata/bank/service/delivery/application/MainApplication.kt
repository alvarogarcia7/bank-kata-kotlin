package com.example.kata.bank.service.delivery.application

import com.example.kata.bank.service.delivery.BankWebApplication
import com.example.kata.bank.service.delivery.StatementRequestInteractor
import com.example.kata.bank.service.delivery.handlers.AccountsHandler
import com.example.kata.bank.service.delivery.handlers.OperationsHandler
import com.example.kata.bank.service.delivery.handlers.UsersHandler
import com.example.kata.bank.service.infrastructure.accounts.AccountRestrictedRepository
import com.example.kata.bank.service.infrastructure.operations.OperationService
import com.example.kata.bank.service.infrastructure.operations.OperationsRepository
import com.example.kata.bank.service.infrastructure.users.UsersSimpleRepository

fun main(args: Array<String>) {
    val accountRepository = AccountRestrictedRepository(mutableListOf())
    val handlers = arrayOf(
            OperationsHandler(OperationService(), accountRepository),
            AccountsHandler(
                    accountRepository,
                    StatementRequestInteractor(accountRepository,
                            OperationsRepository())),
            UsersHandler(UsersSimpleRepository()))
    BankWebApplication(*handlers).start(8080)
}