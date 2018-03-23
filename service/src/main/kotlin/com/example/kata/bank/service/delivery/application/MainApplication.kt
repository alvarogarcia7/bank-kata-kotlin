package com.example.kata.bank.service.delivery.application

import com.example.kata.bank.service.delivery.*
import com.example.kata.bank.service.domain.accounts.AccountRestrictedRepository
import com.example.kata.bank.service.domain.users.UsersSimpleRepository
import com.example.kata.bank.service.infrastructure.OperationsSimpleRepository
import com.example.kata.bank.service.infrastructure.operations.OperationService

fun main(args: Array<String>) {
    val accountRepository = AccountRestrictedRepository()
    BankWebApplication(OperationsHandler(OperationService(), accountRepository), AccountsHandler(
            accountRepository,
            XAPPlicationService(accountRepository,
                    OperationsSimpleRepository())),
            UsersHandler(UsersSimpleRepository())
    ).start(8080)
}