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
import java.util.*

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
        http.get("/users/:userId/operations/:operationId", function = operationsHandler.get)
//        http.post("/users/:userId/operations", function = { req: spark.Request, res: spark.Response -> }) // send the userId parameter explicitly here
    }

    override fun stop() {
        http.stop()
    }
}

class OperationsHandler(private val operationService: OperationService) {
    val mapper = Mapper()
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
                operationRequest.let {
                    AccountLocator.`for`(UserId(userId))
                            .flatMap { account ->
                                operationService.deposit(account, it)
                            }.map {
                                val operationId = it.toString()
                                result = objectMapper.writeValueAsString(MyResponse("", listOf(Link("/users/$userId/operations/$operationId", "list", "GET"))))
                            }
                        }
            }
        }
        response.status(200)
        result
    }

    val get: RouteHandler.() -> String = {
        val userId: String? = request.params(":userId")
        val operationId: String? = request.params(":operationId")
        if (userId == null || operationId == null) {
            throw RuntimeException("invalid request") //TODO AGB
        }
        val objectMapper = JSONMapper.aNew()

        var result = ""
        AccountLocator.`for`(UserId(userId))
                .flatMap {
                    it.find(UUID.fromString(operationId))
                }.map {
                    result = objectMapper.writeValueAsString(MyResponse(mapper.toDTO(it.value), listOf(Link("/users/$userId/operations/$operationId", "self", "GET"))))
                }

        result

    }
}