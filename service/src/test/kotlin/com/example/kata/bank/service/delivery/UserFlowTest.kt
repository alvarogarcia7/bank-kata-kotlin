package com.example.kata.bank.service.delivery.e2e

import com.example.kata.bank.service.ApplicationBooter
import com.example.kata.bank.service.HTTP
import com.example.kata.bank.service.delivery.AccountsHandlerClient
import com.example.kata.bank.service.delivery.BankWebApplication
import com.example.kata.bank.service.delivery.`in`.StatementRequestDTO
import com.example.kata.bank.service.delivery.application.ApplicationEngine
import com.example.kata.bank.service.delivery.handlers.AccountsHandler
import com.example.kata.bank.service.delivery.handlers.OperationsHandler
import com.example.kata.bank.service.delivery.handlers.UsersHandler
import com.example.kata.bank.service.delivery.json.MyResponse
import com.example.kata.bank.service.delivery.out.StatementOutDTO
import com.example.kata.bank.service.domain.Id
import com.example.kata.bank.service.domain.Persisted
import com.example.kata.bank.service.domain.transactions.Transaction
import com.example.kata.bank.service.infrastructure.accounts.AccountRestrictedRepository
import com.example.kata.bank.service.infrastructure.accounts.out.AccountDTO
import com.example.kata.bank.service.infrastructure.operations.AmountDTO
import com.example.kata.bank.service.infrastructure.operations.OperationsRepository
import com.example.kata.bank.service.infrastructure.users.UsersSimpleRepository
import com.example.kata.bank.service.usecases.accounts.DepositUseCase
import com.example.kata.bank.service.usecases.accounts.OpenAccountUseCase
import com.example.kata.bank.service.usecases.accounts.TransferUseCase
import com.example.kata.bank.service.usecases.statements.StatementCreationUseCase
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.core.Request
import com.github.kittinunf.fuel.core.Response
import com.github.kittinunf.result.Result
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.platform.runner.JUnitPlatform
import org.junit.runner.RunWith


@RunWith(JUnitPlatform::class)
class UserFlowTest {

    val http = HTTP

    companion object {
        private var application: ApplicationEngine? = null

        @AfterAll
        @JvmStatic
        fun stop() {
            application?.stop()
        }

        @BeforeAll
        @JvmStatic
        fun setup() {
            val (application, serverPort) = ApplicationBooter(configuredApplication).atRandomPort()
            this.application = application
            configurePort(serverPort)
        }

        fun configurePort(serverPort: Int) {
            FuelManager.instance.basePath = "http://localhost:" + serverPort
        }

        val operationsRepository = OperationsRepository()

        val accountRepository = AccountRestrictedRepository.aNew()
        private val configuredApplication: () -> BankWebApplication = {
            BankWebApplication(
                    OperationsHandler(
                            accountRepository,
                            TransferUseCase(accountRepository),
                            DepositUseCase(accountRepository),
                            operationsRepository),
                    AccountsHandler(accountRepository, StatementCreationUseCase(operationsRepository), OpenAccountUseCase(accountRepository)),
                    UsersHandler(UsersSimpleRepository()))
        }
    }


    @Test
    fun `get the balance after a few deposits`() {
        val accountId = createAccount()
        deposit10(accountId)
        deposit10(accountId)
        deposit10(accountId)
        deposit10(accountId)
        val statementUri = `create statement and get its uri`(accountId)
        val response = `fetch statement`(statementUri)
        assertThat(response.response.statementLines.first().balance).isEqualTo(AmountDTO.EUR("" + (4 * 10 - 1) + ".00"))
    }

    private fun `fetch statement`(statementUri: String) =
            http.get(statementUri).let(http::request).let(readAsStatementDto)

    private fun `create statement and get its uri`(accountId: Id) =
            createStatement(accountId, StatementRequestDTO("statement")).let(http::request).let(readAsAny).links.first { it.rel == "self" }.href

    private fun createAccount(): Id {
        val jsonPayload = AccountsHandlerClient.createAccount("john doe")
        return Id.of((HTTP::post)("/accounts", jsonPayload).let(http::request).let(readAsAccountDto).let { it -> it.links.first { it.rel == "self" }.href.split("/").last() })
    }



    val readAsAccountDto: (Pair<Response, Result.Success<String, FuelError>>) -> MyResponse<AccountDTO> = { (_, result) -> http.mapper.readValue(result.value) }
    val readAsStatementDto: (Pair<Response, Result.Success<String, FuelError>>) -> MyResponse<StatementOutDTO> = { (_, result) -> http.mapper.readValue(result.value) }
    val readAsAny: (Pair<Response, Result.Success<String, FuelError>>) -> MyResponse<Any> = { (_, result) -> http.mapper.readValue(result.value) }

    fun bIsSupersetOfA(a: List<Persisted<Transaction>>, b: List<Persisted<Transaction>>) {
        a.forEach {
            b.contains(it)
        }
    }


    private fun deposit10(accountId: Id) {
        depositRequest(accountId, AccountsHandlerClient.deposit("10")).let(http::request)
    }


    private fun depositRequest(accountId: Id, jsonPayload: String): Request {
        return http.post("accounts/${accountId.value}/operations", jsonPayload)
    }

    private fun createStatement(value: Id, request: StatementRequestDTO): Request {
        return http.post("/accounts/${value.value}", request)
    }

}

