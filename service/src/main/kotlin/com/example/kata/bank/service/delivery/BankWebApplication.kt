package com.example.kata.bank.service.delivery

import arrow.core.*
import com.example.kata.bank.service.delivery.application.ApplicationEngine
import com.example.kata.bank.service.delivery.json.JSONMapper
import com.example.kata.bank.service.delivery.json.MyResponse
import com.example.kata.bank.service.delivery.json.hateoas.Link
import com.example.kata.bank.service.domain.Account
import com.example.kata.bank.service.domain.Clock
import com.example.kata.bank.service.domain.Persisted
import com.example.kata.bank.service.domain.User
import com.example.kata.bank.service.infrastructure.HelloRequest
import com.example.kata.bank.service.infrastructure.HelloService
import com.example.kata.bank.service.infrastructure.operations.OperationRequest
import com.example.kata.bank.service.infrastructure.operations.OperationService
import com.fasterxml.jackson.module.kotlin.readValue
import spark.kotlin.Http
import spark.kotlin.RouteHandler
import spark.kotlin.ignite
import java.util.*

class BankWebApplication(
        private val helloService: HelloService,
        private val operationsHandler: OperationsHandler,
        private val accountsHandler: AccountsHandler,
        private val usersHandler: UsersHandler) :
        ApplicationEngine {
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

        //accounts
        http.get("/accounts", function = accountsHandler.list)
        http.post("/accounts", function = accountsHandler.add)
        http.get("/accounts/:accountId", function = accountsHandler.detail)

        //operations
        http.get("/accounts/:accountId/operations/:operationId", function = operationsHandler.get)
        http.post("/accounts/:accountId/operations", function = operationsHandler.add)

        //users
        http.get("/users", function = usersHandler.list)
//        http.post("/users/:userId/operations", function = { req: spark.Request, res: spark.Response -> }) // send the userId parameter explicitly here
    }

    override fun stop() {
        http.stop()
    }
}


class AccountsHandler(private val accountRepository: AccountRepository) {
    private val mapper = Mapper()
    private val objectMapper = JSONMapper.aNew()
    val list: RouteHandler.() -> String = {
        val x = accountRepository
                .findAll()
                .map { Pair(it.id, toDTO(it.value)) }
                .map { (id, account) -> MyResponse(account, listOf(Link("/accounts/$id", rel = "self", method = "GET"))) }
        objectMapper.writeValueAsString(x)
    }

    val add: RouteHandler.() -> String = {
        val openAccountRequestDTO = objectMapper.readValue<OpenAccountRequestDTO>(request.body())
        val result = openAccountRequestDTO
                .validate()
                .flatMap { OpenAccountRequest.parse(it.name!!) }
                .map { Persisted.`for`(it, UUID.randomUUID()) }
                .map {
                    accountRepository.save(it)
                    it
                }
                .map { Pair(it.id, toDTO(it.value)) }
                .map { (id, account) -> MyResponse(account, listOf(Link("/accounts/$id", rel = "self", method = "GET"))) }
        when (result) {
            is Either.Right -> {
                objectMapper.writeValueAsString(result.b)
            }
            is Either.Left -> {
                response.status(400)
                objectMapper.writeValueAsString("")
            }
        }
    }

    val detail: RouteHandler.() -> String = {
        val accountId: String = request.params(":accountId") ?: throw RuntimeException("null account") //TODO AGB
        val result = accountRepository.findBy(AccountId(accountId))
                .map { Pair(it.id, toDTO(it.value)) }
                .map { (id, account) -> MyResponse(account, listOf(Link("/accounts/$id", rel = "self", method = "GET"))) }
        when (result) {
            is None -> {
                response.status(400)
                objectMapper.writeValueAsString("")
            }
            is Some -> {
                objectMapper.writeValueAsString(result.t)
            }
        }
    }

    private fun toDTO(account: Account): AccountDTO {
        return mapper.toDTO(account)
    }
}

data class OpenAccountRequest private constructor(val name: String) {
    companion object {
        fun parse(name: String): Either<List<Error>, Account> {
            return Either.right(Account(Clock.aNew(), name))
        }
    }
}

data class OpenAccountRequestDTO(val name: String?) {
    fun validate(): Either<List<Exception>, OpenAccountRequestDTO> {
        var errors = mutableListOf<Exception>()
        if (name == null || name == "") {
            errors.add(IllegalArgumentException("empty/blank account name"))
        }
        return if (errors.isEmpty()) {
            Either.right(this)
        } else {
            Either.left(errors)
        }
    }
}


class UsersHandler(private val usersRepository: UsersRepository) {
    private val mapper = Mapper()
    private val objectMapper = JSONMapper.aNew()
    val list: RouteHandler.() -> String = {
        val x = usersRepository
                .findAll()
                .map { Pair(it.id, mapper.toDTO(it.value)) }
                .map { (id, user) -> MyResponse(user, listOf(Link("/users/$id", rel = "self", method = "GET"))) }
        objectMapper.writeValueAsString(x)
    }

}


class UsersRepository : InMemoryRepository<User>()

open class InMemoryRepository<X> {
    private val values = mutableListOf<Persisted<X>>()

    fun save(entity: Persisted<X>) {
        this.values.add(entity)
    }

    fun findAll(): List<Persisted<X>> {
        return values.toList()
    }
}


class OperationsHandler(private val operationService: OperationService, private val accountRepository: AccountRepository) {
    private val mapper = Mapper()

    val add: RouteHandler.() -> String = {
        val accountId: String? = request.params(":accountId")
        if (accountId == null) {
            throw RuntimeException("null account") //TODO AGB
        }
        val objectMapper = JSONMapper.aNew()
        val operationRequest = objectMapper.readValue<OperationRequest>(request.body())
        var result = ""
        when (operationRequest) {
            is OperationRequest.DepositRequest -> {
                operationRequest.let {
                    accountFor(accountId)
                            .flatMap { account ->
                                operationService.deposit(account, it)
                            }.map {
                                val operationId = it.toString()
                                result = objectMapper.writeValueAsString(MyResponse("", listOf(Link("/accounts/$accountId/operations/$operationId", "list", "GET"))))
                            }
                }
            }
        }
        response.status(200)
        result
    }

    val get: RouteHandler.() -> String = {
        val accountId: String? = request.params(":accountId")
        val operationId: String? = request.params(":operationId")
        if (accountId == null || operationId == null) {
            throw RuntimeException("invalid request") //TODO AGB
        }
        val objectMapper = JSONMapper.aNew()

        var result = ""
        accountFor(accountId)
                .flatMap {
                    it.find(UUID.fromString(operationId))
                }.map {
                    result = objectMapper.writeValueAsString(MyResponse(mapper.toDTO(it.value), listOf(Link("/accounts/$accountId/operations/$operationId", "self", "GET"))))
                }

        result

    }

    private fun accountFor(accountId: String) = accountRepository.findBy(AccountId(accountId)).map { it.value }
}

class AccountRepository {
    private val accounts = mutableListOf<Persisted<Account>>()

    fun findBy(accountId: AccountId): Option<Persisted<Account>> {
        return Option.fromNullable(accounts.find { it.id == UUID.fromString(accountId.value) })
    }

    fun save(entity: Persisted<Account>) {
        this.accounts.add(entity)
    }

    fun findAll(): List<Persisted<Account>> {
        return accounts.toList()
    }
}

data class AccountId(val value: String)
