package com.example.kata.bank.service.delivery.handlers

import arrow.core.Either
import arrow.core.Option
import arrow.core.flatMap
import com.example.kata.bank.service.delivery.BankWebApplication.Companion.canFail
import com.example.kata.bank.service.delivery.BankWebApplication.Companion.many
import com.example.kata.bank.service.delivery.BankWebApplication.Companion.mayBeMissing
import com.example.kata.bank.service.delivery.X
import com.example.kata.bank.service.delivery.`in`.OpenAccountRequestDTO
import com.example.kata.bank.service.delivery.`in`.StatementRequestDTO
import com.example.kata.bank.service.delivery.json.JSONMapper
import com.example.kata.bank.service.delivery.json.MyResponse
import com.example.kata.bank.service.delivery.json.hateoas.Link
import com.example.kata.bank.service.delivery.out.ErrorsDTO
import com.example.kata.bank.service.domain.Id
import com.example.kata.bank.service.infrastructure.accounts.AccountRestrictedRepository
import com.example.kata.bank.service.infrastructure.accounts.out.AccountDTO
import com.example.kata.bank.service.infrastructure.mapper.Mapper
import com.example.kata.bank.service.usecases.accounts.OpenAccountUseCase
import com.example.kata.bank.service.usecases.statements.StatementCreationUseCase
import com.fasterxml.jackson.module.kotlin.readValue
import spark.kotlin.Http

class AccountsHandler(private val accountRepository: AccountRestrictedRepository, private val statementCreationUseCase: StatementCreationUseCase, val openAccountUseCase: OpenAccountUseCase) : Handler {
    override fun register(http: Http) {
        http.get("/accounts", function = many(::list))
        http.post("/accounts", function = canFail(::add))
        http.get("/accounts/:accountId", function = mayBeMissing(::detail))
        http.post("/accounts/:accountId", function = canFail(::request))
    }

    private val mapper = Mapper()
    private val objectMapper = JSONMapper.aNew()
    fun list(request: spark.Request, response: spark.Response): X.ResponseEntity<List<MyResponse<AccountDTO>>> {
        val payload = accountRepository
                .findAll()
                .map { (account, id) -> Pair(mapper.toDTO(account), id) }
                .map { (account, id) -> MyResponse.links(account, Link.self(Pair("accounts", id))) }
        return X.ok(payload)
    }

    fun add(request: spark.Request, response: spark.Response): Either<X.ResponseEntity<MyResponse<List<String>>>, X.ResponseEntity<MyResponse<AccountDTO>>> {
        val openAccountRequestDTO = objectMapper.readValue<OpenAccountRequestDTO>(request.body())
        return openAccountRequestDTO
                .validate()
                .flatMap { it -> openAccountUseCase.open(it) }
                .map { (account, id) ->
                    MyResponse(mapper.toDTO(account), listOf(Link.self("accounts" to id)))
                }
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
                                val statementId = statementCreationUseCase.createStatement(account.value, it)
                                MyResponse.links("", Link.self(Pair("accounts", id), Pair("statements", statementId)))
                            }

                    Either.cond(payload.isDefined(), { payload.get() }, { listOf(Exception("Account does not exist")) })
                }
                .mapLeft { X.badRequest(MyResponse.noLinks(ErrorsDTO.from(it))) }
                .map { X.ok(it) }
    }
}