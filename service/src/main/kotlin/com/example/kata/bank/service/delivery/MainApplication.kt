package com.example.kata.bank.service.delivery

import com.example.kata.bank.service.infrastructure.HelloService
import com.example.kata.bank.service.infrastructure.operations.OperationService

fun main(args: Array<String>) {
    BankWebApplication(HelloService(), OperationService()).start(8080)
}