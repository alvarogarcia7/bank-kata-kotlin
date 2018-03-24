package com.example.kata.bank.service.delivery.application

import com.example.kata.bank.service.delivery.BankWebApplication
import com.example.kata.bank.service.delivery.handlers.AccountsHandler
import com.example.kata.bank.service.delivery.handlers.OperationsHandler
import com.example.kata.bank.service.delivery.handlers.UsersHandler
import com.example.kata.bank.service.infrastructure.accounts.AccountRestrictedRepository
import com.example.kata.bank.service.infrastructure.operations.OperationService
import com.example.kata.bank.service.infrastructure.operations.OperationsRepository
import com.example.kata.bank.service.infrastructure.users.UsersSimpleRepository
import com.example.kata.bank.service.usecases.accounts.OpenAccountUseCase
import com.example.kata.bank.service.usecases.statements.StatementCreationUseCase

fun main(args: Array<String>) {
    val accountRepository = AccountRestrictedRepository(mutableListOf())
    val handlers = arrayOf(
            OperationsHandler(OperationService(), accountRepository),
            AccountsHandler(
                    accountRepository,
                    StatementCreationUseCase(OperationsRepository()),
                    OpenAccountUseCase(accountRepository)),
            UsersHandler(UsersSimpleRepository()))
    BankWebApplication(*handlers).start(8080)
}