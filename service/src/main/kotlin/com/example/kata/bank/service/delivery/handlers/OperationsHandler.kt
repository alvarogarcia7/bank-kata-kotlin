package com.example.kata.bank.service.delivery.handlers

import arrow.core.Either
import arrow.core.Option
import arrow.core.flatMap
import com.example.kata.bank.service.NotTestedOperation
import com.example.kata.bank.service.delivery.BankWebApplication.Companion.canFail
import com.example.kata.bank.service.delivery.BankWebApplication.Companion.many
import com.example.kata.bank.service.delivery.BankWebApplication.Companion.mayBeMissing
import com.example.kata.bank.service.delivery.X
import com.example.kata.bank.service.delivery.json.JSONMapper
import com.example.kata.bank.service.delivery.json.MyResponse
import com.example.kata.bank.service.delivery.json.hateoas.Link
import com.example.kata.bank.service.delivery.json.readValueOption
import com.example.kata.bank.service.delivery.out.ErrorsDTO
import com.example.kata.bank.service.delivery.out.StatementOutDTO
import com.example.kata.bank.service.domain.Id
import com.example.kata.bank.service.domain.accounts.Account
import com.example.kata.bank.service.domain.transactions.Amount
import com.example.kata.bank.service.infrastructure.accounts.AccountRestrictedRepository
import com.example.kata.bank.service.infrastructure.mapper.Mapper
import com.example.kata.bank.service.infrastructure.operations.OperationService
import com.example.kata.bank.service.infrastructure.operations.`in`.OperationRequest
import com.example.kata.bank.service.infrastructure.operations.out.TransactionDTO
import spark.kotlin.Http

class OperationsHandler(private val operationService: OperationService, private val accountRepository: AccountRestrictedRepository) : Handler {
    override fun register(http: Http) {
        http.get("/accounts/:accountId/operations/:operationId", function = mayBeMissing(::detail))
        http.get("/accounts/:accountId/operations", function = many(::list))
        http.get("/accounts/:accountId/statements/:statementId", function = canFail(::getStatement))
        http.post("/accounts/:accountId/operations", function = canFail(::add))
    }

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