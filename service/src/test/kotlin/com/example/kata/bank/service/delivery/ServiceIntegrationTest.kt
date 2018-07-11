package com.example.kata.bank.service.delivery

import arrow.core.Either
import arrow.core.getOrElse
import com.example.kata.bank.service.ApplicationBooter
import com.example.kata.bank.service.HTTP
import com.example.kata.bank.service.UnreachableCode
import com.example.kata.bank.service.delivery.application.ApplicationEngine
import com.example.kata.bank.service.delivery.handlers.AccountsHandler
import com.example.kata.bank.service.delivery.handlers.OperationsHandler
import com.example.kata.bank.service.delivery.handlers.UsersHandler
import com.example.kata.bank.service.domain.Id
import com.example.kata.bank.service.domain.Persisted
import com.example.kata.bank.service.domain.accounts.Account
import com.example.kata.bank.service.domain.accounts.Clock
import com.example.kata.bank.service.domain.transactions.Amount
import com.example.kata.bank.service.infrastructure.accounts.AccountRestrictedRepository
import com.example.kata.bank.service.infrastructure.operations.OperationsRepository
import com.example.kata.bank.service.infrastructure.users.UsersSimpleRepository
import com.example.kata.bank.service.usecases.accounts.DepositUseCase
import com.example.kata.bank.service.usecases.accounts.OpenAccountUseCase
import com.example.kata.bank.service.usecases.accounts.TransferUseCase
import com.example.kata.bank.service.usecases.statements.StatementCreationUseCase
import com.github.kittinunf.fuel.core.FuelManager
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyZeroInteractions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.platform.runner.JUnitPlatform
import org.junit.runner.RunWith
import org.mockito.Mockito

@RunWith(JUnitPlatform::class)
class ServiceIntegrationTest {

    object Mocks {
        @JvmStatic
        val depositUseCase = Mockito.mock(DepositUseCase::class.java)
    }

    @BeforeEach
    fun resetMocks() {
        Mockito.reset(Mocks.depositUseCase)
    }

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

        class MockOperationService : DepositUseCase(accountRepository) {
            override fun deposit(accountId: Id, request: Request): Either<List<Exception>, Id> {
                Mocks.depositUseCase.deposit(accountId, request)
                return super.deposit(accountId, request)
            }
        }

        val accountRepository = AccountRestrictedRepository.aNew()
        val transferUseCase: TransferUseCase = TransferUseCase(accountRepository)
        private val configuredApplication: () -> BankWebApplication = {
            BankWebApplication(
                    OperationsHandler(accountRepository, transferUseCase, MockOperationService(), OperationsRepository()),
                    AccountsHandler(accountRepository, StatementCreationUseCase(OperationsRepository()), OpenAccountUseCase(accountRepository)),
                    UsersHandler(UsersSimpleRepository())
            )
        }
    }


    @Test
    fun `deposit into an existing account`() {

        val accountId = Id.random()
        accountRepository.save(Persisted.`for`(Account(Clock.aNew(), "account name"), accountId))

        val description = "first deposit into account"
        deposit(accountId, """
{
    "type": "deposit",
    "amount": {
        "value": "100.00",
        "currency": "EUR"
    },
    "description": "$description"
}
            """)
                .let(http::request)
                .let { (response, result) ->
                    println(result)
                    assertThat(response.statusCode).isEqualTo(200)

                    val account = accountRepository.findBy(accountId).map { it.value }.getOrElse { throw UnreachableCode() }
                    verify(Mocks.depositUseCase).deposit(accountId, DepositUseCase.Request(Amount.of("100.00"), description))
                }
    }

    @Test
    fun `deposit - invalid request`() {
        val accountId = Id.random()
        accountRepository.save(Persisted.`for`(Account(Clock.aNew(), "account name"), accountId))

        deposit(accountId, """
{
    "amount": {
        "value": "1234.56",
        "currency": "EUR"
    },
    "description": "rent for this month"
}
            """) //missing the type
                .let { http.assertFailedRequest(it, http::assertError) }
                .let { (response, result) ->
                    println(result)
                    assertThat(response.statusCode).isEqualTo(400)

                    verifyZeroInteractions(Mocks.depositUseCase)
                }
    }

    val http = HTTP

    private fun deposit(toAccount: Id, request: String) = HTTP.post("/accounts/${toAccount.value}/operations", request)
}
