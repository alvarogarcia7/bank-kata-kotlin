package com.example.kata.bank.service.delivery.application

import com.example.kata.bank.service.delivery.*
import com.example.kata.bank.service.infrastructure.HelloService
import com.example.kata.bank.service.infrastructure.operations.OperationService

fun main(args: Array<String>) {
    val accountRepository = AccountRepository()
    BankWebApplication(HelloService(), OperationsHandler(OperationService(), accountRepository), AccountsHandler(accountRepository), UsersHandler(UsersRepository())).start(8080)
}