package com.example.kata.bank.service.delivery

import arrow.core.*
import com.example.kata.bank.service.NotTestedOperation
import com.example.kata.bank.service.delivery.application.ApplicationEngine
import com.example.kata.bank.service.delivery.json.JSONMapper
import com.example.kata.bank.service.delivery.json.MyResponse
import com.example.kata.bank.service.delivery.json.hateoas.Link
import com.example.kata.bank.service.delivery.json.readValueOption
import com.example.kata.bank.service.domain.AccountRequest
import com.example.kata.bank.service.domain.Id
import com.example.kata.bank.service.domain.Operation
import com.example.kata.bank.service.domain.Persisted
import com.example.kata.bank.service.domain.accounts.Account
import com.example.kata.bank.service.domain.accounts.AccountRestrictedRepository
import com.example.kata.bank.service.domain.accounts.OpenAccountRequest
import com.example.kata.bank.service.domain.transactions.Amount
import com.example.kata.bank.service.domain.users.UsersSimpleRepository
import com.example.kata.bank.service.infrastructure.OperationsSimpleRepository
import com.example.kata.bank.service.infrastructure.accounts.AccountDTO
import com.example.kata.bank.service.infrastructure.mapper.Mapper
import com.example.kata.bank.service.infrastructure.operations.OperationRequest
import com.example.kata.bank.service.infrastructure.operations.OperationService
import com.example.kata.bank.service.infrastructure.operations.TransactionDTO
import com.example.kata.bank.service.infrastructure.statement.Statement
import com.fasterxml.jackson.module.kotlin.readValue
import spark.Request
import spark.Response
import spark.Service
import spark.kotlin.Http
import spark.kotlin.RouteHandler
import kotlin.reflect.KFunction2

class BankWebApplication(
        private val operationsHandler: OperationsHandler,
        private val accountsHandler: AccountsHandler,
        private val usersHandler: UsersHandler) :
        ApplicationEngine {
    private var httpService: Http = Http(Service.ignite())

    override fun start(port: Int): BankWebApplication {
        val http = httpService
                .port(port)
//                .threadPool(10)

        configurePaths(http)
        return this
    }

    private fun configurePaths(http: Http) {
        //accounts
        http.get("/accounts", function = list(accountsHandler::list))
        http.post("/accounts", function = canFail(accountsHandler::add))
        http.get("/accounts/:accountId", function = mayBeMissing(accountsHandler::detail))
        http.post("/accounts/:accountId", function = canFail(accountsHandler::request))

//        operations
        http.get("/accounts/:accountId/operations/:operationId", function = mayBeMissing(operationsHandler::detail))
        http.get("/accounts/:accountId/operations", function = list(operationsHandler::list))
        http.get("/accounts/:accountId/statements/:statementId", function = canFail(operationsHandler::getStatement))
        http.post("/accounts/:accountId/operations", function = canFail(operationsHandler::add))

        //users
        http.get("/users", function = usersHandler.list)
    }

    private val objectMapper = JSONMapper.aNew()
    private fun <T : Any> list(kFunction2: KFunction2<Request, Response, X.ResponseEntity<T>>): RouteHandler.() -> Any = {
        val result = kFunction2.invoke(request, response)
        response.status(result.statusCode)
        result.payload
                .orElse { Some("") }
                .map { objectMapper.writeValueAsString(it) }.get()
    }

    private fun <T : Any, S : Any> canFail(fn: KFunction2<Request, Response, Either<X.ResponseEntity<T>, X.ResponseEntity<S>>>): RouteHandler.() -> Any = {
        val result = fn.invoke(request, response)
        val payload = when (result) {
            is Either.Left -> {
                response.status(result.a.statusCode)
                result.a.payload
            }
            is Either.Right -> {
                response.status(result.b.statusCode)
                result.b.payload
            }
        }
        val body = when (payload) {
            is Some -> serialize(payload.t)
            is None -> ""
        }
        body
    }

    private fun <T : Any> mayBeMissing(fn: KFunction2<Request, Response, Option<X.ResponseEntity<T>>>): RouteHandler.() -> Any = {
        val result = fn.invoke(request, response)
        when (result) {
            is Some -> {
                response.status(result.t.statusCode)
                val nP = result.t.payload
                when (nP) {
                    is Some -> serialize(nP.t)
                    is None -> NotTestedOperation()
                }
            }
            is None -> {
                response.status(404)
                ""
            }
        }
    }

    private fun <T> serialize(it: T): String {
        return objectMapper.writeValueAsString(it)
    }

    override fun stop() {
        httpService.stop()
    }
}


class AccountsHandler(private val accountRepository: AccountRestrictedRepository, private val xApplicationService: XAPPlicationService) {
    private val mapper = Mapper()
    private val objectMapper = JSONMapper.aNew()
    fun list(request: spark.Request, response: spark.Response): X.ResponseEntity<List<MyResponse<AccountDTO>>> {
        val payload = accountRepository
                .findAll()
                .map { (account, id) -> MyResponse.links(mapper.toDTO(account), Link.self(Pair("accounts", id))) }
        return X.ok(payload)
    }

    fun add(request: spark.Request, response: spark.Response): Either<X.ResponseEntity<MyResponse<List<String>>>, X.ResponseEntity<MyResponse<AccountDTO>>> {
        val openAccountRequestDTO = objectMapper.readValue<OpenAccountRequestDTO>(request.body())
        return (openAccountRequestDTO
                .validate()
                .flatMap { OpenAccountRequest.parse(it.name!!) }
                .map { Persisted.`for`(it, Id.random()) }
                .map {
                    accountRepository.save(it)
                    it
                }
                .map { (account, id) ->
                    MyResponse(mapper.toDTO(account), listOf(Link.self("accounts" to id)))
                })
                .mapLeft { it -> MyResponse(it.map { it.message!! }, listOf()) }
                .mapLeft { it -> X.badRequest(it) }
                .map { it -> X.ok(it) }
    }

    fun detail(request: spark.Request, response: spark.Response): Option<X.ResponseEntity<MyResponse<AccountDTO>>> {
        val accountId: String = request.params(":accountId") ?: throw RuntimeException("null account") //TODO AGB
        return accountRepository.findBy(Id.of(accountId))
                .map { (account, id) -> MyResponse.links(mapper.toDTO(account), Link.self(Pair("accounts", id))) }
                .map { it -> X.ok(it) }
    }

    fun request(request: spark.Request, response: spark.Response): Either<X.ResponseEntity<MyResponse<ErrorsDTO>>, X.ResponseEntity<MyResponse<String>>> {

        val accountId: String = request.params(":accountId") ?: throw RuntimeException("null account") //TODO AGB
        val statementRequestDTO = objectMapper.readValue<StatementRequestDTO>(request.body())
        return statementRequestDTO.validate()
                .flatMap {
                    val payload = accountRepository
                            .findBy(Id.of(accountId))
                            .map { account ->
                                val id = account.id
                                val statementId = xApplicationService.createAndSaveOperation(account.value, StatementRequestFactory.create(it))
                                MyResponse.links("", Link.self(Pair("accounts", id), Pair("statements", statementId)))
                            }

                    Either.cond(payload.isDefined(), { payload.get() }, { listOf(Exception("Account does not exist")) })
                }
                .mapLeft { X.badRequest(MyResponse.noLinks(ErrorsDTO.from(it))) }
                .map { X.ok(it) }
    }
}

class X {
    companion object {
        fun <T> ok(payload: T): ResponseEntity<T> {
            return ResponseEntity(200, Some(payload))
        }

        fun <T> either(map: Either<List<Exception>, MyResponse<T>>): Either<ResponseEntity<List<String>>, ResponseEntity<MyResponse<T>>> {
            return map.bimap({ X.error(it) }, { X.ok(it) })
        }

        private fun error(it: List<Exception>): ResponseEntity<List<String>> {
            return ResponseEntity(400, Some(it.map { it.message!! }))
        }

        fun <T> badRequest(it: T): ResponseEntity<T> {
            return ResponseEntity(400, Some(it))
        }
    }

    data class ResponseEntity<out T>(val statusCode: Int, val payload: Option<T>)
}

class StatementRequestFactory {
    companion object {
        fun create(request: StatementRequestDTO): AccountRequest {
            return AccountRequest.StatementRequest.all()
        }
    }
}

class XAPPlicationService(val accountRepository: AccountRestrictedRepository, val operationsRepository: OperationsSimpleRepository) {
    fun createAndSaveOperation(account: Account, create: AccountRequest): Id {
        val statement = create.apply<Statement>(account)
        val id = Id.random()
        operationsRepository.save(Persisted.`for`(Operation.Statement(statement), id))
        return id
    }
}


class UsersHandler(private val usersRepository: UsersSimpleRepository) {
    private val objectMapper = JSONMapper.aNew()
    val list: RouteHandler.() -> String = {
        val result = usersRepository
                .findAll()
                .map { (user, id) -> MyResponse.links(user, Link.self(Pair("users", id))) }
        objectMapper.writeValueAsString(result)
    }

}


class OperationsHandler(private val operationService: OperationService, private val accountRepository: AccountRestrictedRepository) {
    private val mapper = Mapper()
    private val objectMapper = JSONMapper.aNew()

    fun add(request: spark.Request, response: spark.Response): Either<X.ResponseEntity<MyResponse<ErrorsDTO>>, X.ResponseEntity<MyResponse<Unit>>> {
        val accountId: String = request.params(":accountId")
                ?: return Either.left(listOf(Exception("Needs an :accountId")))
                        .mapLeft { ErrorsDTO.from(it) }
                        .mapLeft { MyResponse.noLinks(it) }
                        .mapLeft { X.badRequest(it) }
        val result = objectMapper.readValueOption<OperationRequest>(request.body())
                .mapLeft { listOf(it) }
                .flatMap { operationRequest ->
                    when (operationRequest) {
                        is OperationRequest.DepositRequest -> {
                            val depositId = accountFor(accountId)
                            val x = Either.cond(depositId.isDefined(), { depositId.get() }, { listOf(Exception("No account")) })
                                    .flatMap { account ->
                                        val id = operationService.deposit(account, operationRequest)
                                        Either.cond(id.isDefined(), { id.get() }, { listOf(Exception("Deposit failed")) })
                                    }
                            x
                        }
                        is OperationRequest.TransferRequest -> {
                            accountRepository
                                    .findBy(Account.Number.of(operationRequest.destination.number))
                                    .flatMap { to ->
                                        accountRepository.findBy(Id.of(accountId))
                                                .map { from ->
                                                    Account.transfer(Amount.of(operationRequest.amount.value), operationRequest.description, from, to)
                                                }
                                    }
                            Either.right(Id.random())
                        }
                    }
                }
                .mapLeft { listOf(Exception("No result")) }
                .mapLeft { ErrorsDTO.from(it) }
                .mapLeft { MyResponse.noLinks(it) }
                .mapLeft { X.badRequest(it) }
                .map { MyResponse(Unit, listOf(Link("/accounts/$accountId/operations/${it.value}", "list", "GET"))) }
                .map { X.ok(it) }

        return result
    }

    fun detail(request: spark.Request, response: spark.Response): Option<X.ResponseEntity<MyResponse<TransactionDTO>>> {
        val accountId: String? = request.params(":accountId")
        val operationId: String? = request.params(":operationId")
        if (accountId == null || operationId == null) {
            throw RuntimeException("invalid request") //TODO AGB
        }

        return accountFor(accountId)
                .flatMap {
                    it.find(Id.of(operationId))
                }.map {
                    X.ok(MyResponse.links(mapper.toDTO(it.value), Link.self(Pair("accounts", Id.of(accountId)), Pair("operations", Id.of(operationId)))))
                }
    }

    fun getStatement(request: spark.Request, response: spark.Response): Either<X.ResponseEntity<MyResponse<ErrorsDTO>>, X.ResponseEntity<MyResponse<StatementOutDTO>>> {
        val accountId: String? = request.params(":accountId")
        val statementId: String? = request.params(":statementId")
        if (accountId == null || statementId == null) {
            throw NotTestedOperation()
        }

        val result = accountFor(accountId)
                .map {
                    val operations = it.findAll().map { it.value }.map { mapper.toDTO(it) }
                    val response = StatementOutDTO(operations)
                    MyResponse.links(response,
                            Link.self(Pair("accounts", Id.of(accountId)), Pair("operations", Id.of(statementId))))
                }
        return Either.cond(result.isDefined(), { result.get() }, { MyResponse(ErrorsDTO.from(listOf(NotTestedOperation())), listOf()) })
                .map { X.ok(it) }
                .mapLeft { X.badRequest(it) }
    }

    fun list(request: spark.Request, response: spark.Response): X.ResponseEntity<List<MyResponse<TransactionDTO>>> {
        val accountId: String = request.params(":accountId") ?: throw RuntimeException("invalid request") //TODO AGB
        val payload = accountRepository
                .findBy(Id.of(accountId))
                .map {
                    it.value.findAll().map { MyResponse(mapper.toDTO(it.value), listOf(Link("/accounts/$accountId/operations/${it.id.value}", rel = "self", method = "GET"))) }
                }.get()
        return X.ok(payload)
    }

    private fun accountFor(accountId: String) = accountRepository.findBy(Id.of(accountId)).map { it.value }
}

private fun <C, D> Option<D>.replace(negative: () -> Either<C, D>): Either<C, D> {
    return negative.invoke()
}

