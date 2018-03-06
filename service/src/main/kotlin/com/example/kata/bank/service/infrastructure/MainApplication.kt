package com.example.kata.bank.service.infrastructure

import com.example.kata.bank.service.infrastructure.application.BankWebApplication
import com.example.kata.bank.service.infrastructure.operations.OperationService

fun main(args: Array<String>) {
    BankWebApplication(HelloService(), OperationService()).start(8080)
}