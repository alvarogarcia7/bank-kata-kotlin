package com.example.kata.bank.service.infrastructure.application

import com.example.kata.bank.service.domain.UserId
import com.example.kata.bank.service.infrastructure.AccountLocator
import com.example.kata.bank.service.infrastructure.HelloRequest
import com.example.kata.bank.service.infrastructure.HelloService
import com.example.kata.bank.service.infrastructure.JSONMapper
import com.example.kata.bank.service.infrastructure.operations.OperationRequest
import com.example.kata.bank.service.infrastructure.operations.OperationService
import com.example.kata.bank.service.web.Link
import com.example.kata.bank.service.web.MyResponse
import com.fasterxml.jackson.module.kotlin.readValue
import spark.kotlin.Http
import spark.kotlin.RouteHandler
import spark.kotlin.ignite

class BankWebApplication(private val helloService: HelloService, private val operationService: OperationService) : ApplicationEngine {
    private var http: Http = ignite()
    private val helloHandler: RouteHandler.() -> String = {
        HelloRequest(request.queryParamOrDefault("name", null))
                .let { helloService.salute(it) }
    }

    val operationsHandler: RouteHandler.() -> String = {
        val userId: String? = request.params(":userId")
        if (userId == null) {
            throw RuntimeException("null user") //TODO AGB
        }
        val objectMapper = JSONMapper.aNew()
        val operationRequest = objectMapper.readValue<OperationRequest>(request.body())
        when (operationRequest) {
            is OperationRequest.DepositRequest -> {
                operationRequest.let { operationService.deposit(AccountLocator.`for`(UserId(userId)), it) }
            }
        }
        response.status(200)
        objectMapper.writeValueAsString(MyResponse("", listOf(Link("/users/1234/operations/223333", "list", "GET"))))
    }

    override fun start(port: Int): BankWebApplication {
        val http = http
                .port(port)
                .threadPool(10)

        http.get("/", function = helloHandler)
        http.post("/users/:userId/operations", function = operationsHandler)
        return this
    }

    override fun stop() {
        http.stop()
    }
}