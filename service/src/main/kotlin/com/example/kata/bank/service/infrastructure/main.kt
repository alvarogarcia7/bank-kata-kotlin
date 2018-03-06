package com.example.kata.bank.service.infrastructure

import com.example.kata.bank.service.domain.Account
import com.example.kata.bank.service.domain.Amount
import com.example.kata.bank.service.domain.Clock
import com.example.kata.bank.service.web.Link
import com.example.kata.bank.service.web.MyResponse
import com.fasterxml.jackson.module.kotlin.readValue
import spark.kotlin.Http
import spark.kotlin.RouteHandler
import spark.kotlin.ignite
import java.util.*

fun main(args: Array<String>) {
    BankWebApplication(HelloService(), OperationService()).start(8080)
}

class BankWebApplication(private val helloService: HelloService, private val operationService: OperationService) : ApplicationEngine {
    private var http: Http = ignite()
    private val helloHandler: RouteHandler.() -> String = {
        HelloRequest(request.queryParamOrDefault("name", null))
                .let { helloService.salute(it) }
    }

    val operationsHandler: RouteHandler.() -> String = {
        val userId = request.params(":userId")
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
        objectMapper.writeValueAsString(MyResponse("", listOf(Link("/users/1234/operations/223333", "list", "GET"))))

    }

    class AccountLocator {
        companion object {
            private val accounts = mapOf(UserId("1234") to Account(Clock.aNew()))
            fun `for`(userId: UserId): Account? {
                return accounts[userId]
            }
        }

    }

    sealed class OperationRequest {
        data class DepositRequest(val amount: AmountDTO, val description: String) : OperationRequest()

        data class AmountDTO private constructor(val value: String) {
            val currency = Currency.getInstance("EUR")

            companion object {
                fun EUR(value: String): AmountDTO {
                    return AmountDTO(value)
                }
            }
        }
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

data class UserId(val value: String)

interface ApplicationEngine {
    fun start(port: Int): ApplicationEngine
    fun stop()
}

data class HelloRequest(val name: String?)

open class HelloService {
    open fun salute(request: HelloRequest): String {
        fun hello(name: String?) = if (null == name) "Hello, world!" else "Hello $name!"
        return hello(request.name)
    }

}

open class OperationService {
    fun deposit(account: Account?, depositRequest: BankWebApplication.OperationRequest.DepositRequest) {
        account?.deposit(toDomain(depositRequest.amount), depositRequest.description)
    }

    private fun toDomain(amount: BankWebApplication.OperationRequest.AmountDTO): Amount {
        return Amount.Companion.of(amount.value)
    }
}