package com.example.kata.bank.service.delivery.application

import com.example.kata.bank.service.delivery.AccountsHandler
import com.example.kata.bank.service.delivery.BankWebApplication
import com.example.kata.bank.service.delivery.OperationsHandler
import com.example.kata.bank.service.delivery.UsersHandler
import com.example.kata.bank.service.infrastructure.HelloService
import com.example.kata.bank.service.infrastructure.accounts.AccountRepository
import com.example.kata.bank.service.infrastructure.accounts.UsersRepository
import com.example.kata.bank.service.infrastructure.operations.OperationService

fun main(args: Array<String>) {
    val accountRepository = AccountRepository()
    BankWebApplication(HelloService(), OperationsHandler(OperationService(), accountRepository), AccountsHandler(accountRepository), UsersHandler(UsersRepository())).start(8080)
}