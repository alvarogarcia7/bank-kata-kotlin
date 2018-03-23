package com.example.kata.bank.service.delivery.application

import com.example.kata.bank.service.delivery.*
import com.example.kata.bank.service.infrastructure.accounts.AccountRestrictedRepository
import com.example.kata.bank.service.infrastructure.operations.OperationService
import com.example.kata.bank.service.infrastructure.operations.OperationsRepository
import com.example.kata.bank.service.infrastructure.users.UsersSimpleRepository

fun main(args: Array<String>) {
    val accountRepository = AccountRestrictedRepository(mutableListOf())
    BankWebApplication(OperationsHandler(OperationService(), accountRepository), AccountsHandler(
            accountRepository,
            XAPPlicationService(accountRepository,
                    OperationsRepository())),
            UsersHandler(UsersSimpleRepository())
    ).start(8080)
}