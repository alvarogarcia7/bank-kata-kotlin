package com.example.kata.bank.service.delivery

import arrow.core.*
import com.example.kata.bank.service.NotTestedOperation
import com.example.kata.bank.service.delivery.application.ApplicationEngine
import com.example.kata.bank.service.delivery.json.JSONMapper
import com.example.kata.bank.service.delivery.json.MyResponse
import com.example.kata.bank.service.delivery.json.hateoas.Link
import com.example.kata.bank.service.domain.Id
import com.example.kata.bank.service.domain.Operation
import com.example.kata.bank.service.domain.Persisted
import com.example.kata.bank.service.domain.accounts.Account
import com.example.kata.bank.service.domain.accounts.AccountRepository
import com.example.kata.bank.service.domain.accounts.OpenAccountRequest
import com.example.kata.bank.service.domain.transactions.Transaction
import com.example.kata.bank.service.domain.users.UsersRepository
import com.example.kata.bank.service.infrastructure.HelloRequest
import com.example.kata.bank.service.infrastructure.HelloService
import com.example.kata.bank.service.infrastructure.OperationsRepository
import com.example.kata.bank.service.infrastructure.accounts.AccountDTO
import com.example.kata.bank.service.infrastructure.mapper.Mapper
import com.example.kata.bank.service.infrastructure.operations.OperationRequest
import com.example.kata.bank.service.infrastructure.operations.OperationService
import com.example.kata.bank.service.infrastructure.statement.Statement
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
//                .threadPool(10)

        configurePaths(http)
        return this
    }

    private fun configurePaths(http: Http) {
        http.get("/", function = helloHandler)

        //accounts
        http.get("/accounts", function = accountsHandler.list)
        http.post("/accounts", function = accountsHandler.add)
        http.get("/accounts/:accountId", function = accountsHandler.detail)
        http.post("/accounts/:accountId", function = accountsHandler.request)

        //operations
        http.get("/accounts/:accountId/operations/:operationId", function = operationsHandler.get)
        http.get("/accounts/:accountId/operations", function = operationsHandler.list)
        http.get("/accounts/:accountId/statements/:statementId", function = operationsHandler.getStatement)
        http.post("/accounts/:accountId/operations", function = operationsHandler.add)

        //users
        http.get("/users", function = usersHandler.list)
//        http.post("/users/:userId/operations", function = { req: spark.Request, res: spark.Response -> }) // send the userId parameter explicitly here
    }

    override fun stop() {
        http.stop()
    }
}


class AccountsHandler(private val accountRepository: AccountRepository, private val xApplicationService: XAPPlicationService) {
    private val mapper = Mapper()
    private val objectMapper = JSONMapper.aNew()
    val list: RouteHandler.() -> String = {
        val x = accountRepository
                .findAll()
                .map { (account, id) -> MyResponse(mapper.toDTO(account), listOf(Link("/accounts/${id.value}", rel = "self", method = "GET"))) }
        objectMapper.writeValueAsString(x)
    }

    val add: RouteHandler.() -> String = {
        val openAccountRequestDTO = objectMapper.readValue<OpenAccountRequestDTO>(request.body())
        val result = openAccountRequestDTO
                .validate()
                .flatMap { OpenAccountRequest.parse(it.name!!) }
                .map { Persisted.`for`(it, Id(UUID.randomUUID().toString())) }
                .map {
                    accountRepository.save(it)
                    it
                }
                .map { (account, id) ->
                    MyResponse(mapper.toDTO(account), listOf(Link("/accounts/${id.value}", rel = "self", method = "GET")))
                }
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
        val result = accountRepository.findBy(Id(accountId))
                .map { (account, id) -> MyResponse(mapper.toDTO(account), listOf(Link("/accounts/${id.value}", rel = "self", method = "GET"))) }
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

    val request: RouteHandler.() -> String = {
        val accountId: String = request.params(":accountId") ?: throw RuntimeException("null account") //TODO AGB
        val statementRequestDTO = objectMapper.readValue<StatementRequestDTO>(request.body())
        val result: Either<List<Exception>, MyResponse<String>> = statementRequestDTO.validate()
                .flatMap {
                    val x = accountRepository
                            .findBy(Id(accountId))
                            .map { account ->
                                val id = account.id
                                val statementId = xApplicationService.createAndSaveOperation(account.value, StatementRequestFactory.create(it))
                                MyResponse("", listOf(Link("/accounts/${id.value}/statements/${statementId.value}", rel = "self", method
                                = "GET")))
                            }

                    Either.cond(x.isDefined(), { x.get() }, { listOf(Exception("Account does not exist")) })
                }
        when (result) {
            is Either.Left -> {
                response.status(400)
                val messages = result.a.map { it.message!! }
                objectMapper.writeValueAsString(MyResponse(ErrorsDTO(messages), listOf()))
            }
            is Either.Right -> {
                objectMapper.writeValueAsString(result.b)
            }
        }
    }




    private fun toDTO(account: Account): AccountDTO {
        return mapper.toDTO(account)
    }
}

class StatementRequestFactory {
    companion object {
        fun create(request: StatementRequestDTO): AccountRequest {
            return AccountRequest.StatementRequest()
        }

    }

}

sealed class AccountRequest {

    abstract fun <T> apply(account: Account): T

    class StatementRequest : AccountRequest() {
        override fun <T> apply(account: Account): T {

            return account.createStatement() as T //TODO AGB make generic
        }

        val filter: (Transaction) -> Boolean = { transaction -> true } //TODO AGB use filter
    }

}

class XAPPlicationService(val accountRepository: AccountRepository, val operationsRepository: OperationsRepository) {
    fun createAndSaveOperation(account: Account, create: AccountRequest): Id {
        val x = create.apply<Statement>(account)
        val id = Id(UUID.randomUUID().toString())
        operationsRepository.save(Persisted.`for`(Operation.Statement(x), id))
        return id
    }
}


class UsersHandler(private val usersRepository: UsersRepository) {
    private val mapper = Mapper()
    private val objectMapper = JSONMapper.aNew()
    val list: RouteHandler.() -> String = {
        val x = usersRepository
                .findAll()
                .map { (user, id) -> MyResponse(user, listOf(Link("/users/$id", rel = "self", method = "GET"))) }
        objectMapper.writeValueAsString(x)
    }

}


class OperationsHandler(private val operationService: OperationService, private val accountRepository: AccountRepository) {
    private val mapper = Mapper()
    private val objectMapper = JSONMapper.aNew()

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
                    it.find(Id(operationId))
                }.map {
                    result = objectMapper.writeValueAsString(MyResponse(mapper.toDTO(it.value), listOf(Link("/accounts/$accountId/operations/$operationId", "self", "GET"))))
                }

        result

    }

    val getStatement: RouteHandler.() -> String = {
        val accountId: String? = request.params(":accountId")
        val statementId: String? = request.params(":statementId")
        if (accountId == null || statementId == null) {
            throw NotTestedOperation()
        }
        val objectMapper = JSONMapper.aNew()

        val result: MyResponse<Any> = accountFor(accountId)
                .map {
                    val operations = it.findAll().map { it.value }.map { mapper.toDTO(it) }
                    val response = StatementOutDTO(operations)
                    MyResponse(response, listOf(Link("/accounts/$accountId/operations/$statementId", "self", "GET")))
                }.getOrElse { MyResponse(ErrorsDTO(listOf(NotTestedOperation().message!!)), listOf()) }
        objectMapper.writeValueAsString(result)
    }

    val list: RouteHandler.() -> String = {
        val accountId: String = request.params(":accountId") ?: throw RuntimeException("invalid request") //TODO AGB
        val result = accountRepository
                .findBy(Id(accountId))
                .map {
                    it.value
                            .findAll()
                            .map { MyResponse(mapper.toDTO(it.value), listOf(Link("/accounts/$accountId/operations/${it.id}", rel = "self", method = "GET"))) }
                }
        when (result) {
            is None -> {
                throw NotTestedOperation()
                response.status(400)
                objectMapper.writeValueAsString("")
            }
            is Some -> {
                objectMapper.writeValueAsString(result.t)
            }
        }
    }

    private fun accountFor(accountId: String) = accountRepository.findBy(Id(accountId)).map { it.value }
}

