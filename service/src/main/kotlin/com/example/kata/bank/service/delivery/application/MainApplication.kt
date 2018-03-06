package com.example.kata.bank.service.delivery.application

import com.example.kata.bank.service.delivery.AccountRepository
import com.example.kata.bank.service.delivery.BankWebApplication
import com.example.kata.bank.service.delivery.OperationsHandler
import com.example.kata.bank.service.infrastructure.HelloService
import com.example.kata.bank.service.infrastructure.operations.OperationService

fun main(args: Array<String>) {
    BankWebApplication(HelloService(), OperationsHandler(OperationService(), AccountRepository())).start(8080)
}