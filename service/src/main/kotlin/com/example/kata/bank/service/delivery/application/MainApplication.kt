package com.example.kata.bank.service.delivery.application

import com.example.kata.bank.service.delivery.AccountRepository
import com.example.kata.bank.service.delivery.AccountsHandler
import com.example.kata.bank.service.delivery.BankWebApplication
import com.example.kata.bank.service.delivery.OperationsHandler
import com.example.kata.bank.service.infrastructure.HelloService
import com.example.kata.bank.service.infrastructure.operations.OperationService

fun main(args: Array<String>) {
    val accountRepository = AccountRepository()
    BankWebApplication(HelloService(), OperationsHandler(OperationService(), accountRepository), AccountsHandler(accountRepository)).start(8080)
}