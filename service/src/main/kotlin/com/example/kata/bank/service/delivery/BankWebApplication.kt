package com.example.kata.bank.service.delivery

import com.example.kata.bank.service.delivery.application.ApplicationEngine
import com.example.kata.bank.service.delivery.json.JSONMapper
import com.example.kata.bank.service.delivery.json.MyResponse
import com.example.kata.bank.service.delivery.json.hateoas.Link
import com.example.kata.bank.service.domain.UserId
import com.example.kata.bank.service.infrastructure.AccountLocator
import com.example.kata.bank.service.infrastructure.HelloRequest
import com.example.kata.bank.service.infrastructure.HelloService
import com.example.kata.bank.service.infrastructure.operations.OperationRequest
import com.example.kata.bank.service.infrastructure.operations.OperationService
import com.fasterxml.jackson.module.kotlin.readValue
import spark.kotlin.Http
import spark.kotlin.RouteHandler
import spark.kotlin.ignite

class BankWebApplication(private val helloService: HelloService, private val operationsHandler: OperationsHandler) : ApplicationEngine {
    private var http: Http = ignite()
    private val helloHandler: RouteHandler.() -> String = {
        HelloRequest(request.queryParamOrDefault("name", null))
                .let { helloService.salute(it) }
    }

    override fun start(port: Int): BankWebApplication {
        val http = http
                .port(port)
                .threadPool(10)

        configurePaths(http)
        return this
    }

    private fun configurePaths(http: Http) {
        http.get("/", function = helloHandler)
        http.post("/users/:userId/operations", function = operationsHandler.add)
//        http.post("/users/:userId/operations", function = { req: spark.Request, res: spark.Response -> }) // send the userId parameter explicitly here
    }

    override fun stop() {
        http.stop()
    }
}

class OperationsHandler(private val operationService: OperationService) {
    val add: RouteHandler.() -> String = {
        val userId: String? = request.params(":userId")
        if (userId == null) {
            throw RuntimeException("null user") //TODO AGB
        }
        val objectMapper = JSONMapper.aNew()
        val operationRequest = objectMapper.readValue<OperationRequest>(request.body())
        var result = ""
        when (operationRequest) {
            is OperationRequest.DepositRequest -> {
                operationRequest
                        .let { operationService.deposit(AccountLocator.`for`(UserId(userId)), it) }
                        .map {
                            val operationId = it.toString()
                            result = objectMapper.writeValueAsString(MyResponse("", listOf(Link("/users/$userId/operations/$operationId", "list", "GET"))))
                        }
            }
        }
        response.status(200)
        result
    }
}