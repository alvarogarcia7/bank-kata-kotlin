package com.example.kata.bank.service.delivery

import arrow.core.Option
import arrow.core.getOrElse
import com.example.kata.bank.service.ApplicationBooter
import com.example.kata.bank.service.HTTP
import com.example.kata.bank.service.delivery.application.ApplicationEngine
import com.example.kata.bank.service.domain.Id
import com.example.kata.bank.service.domain.Persisted
import com.example.kata.bank.service.domain.accounts.Account
import com.example.kata.bank.service.domain.accounts.AccountRepository
import com.example.kata.bank.service.domain.accounts.Clock
import com.example.kata.bank.service.domain.users.UsersRepository
import com.example.kata.bank.service.infrastructure.OperationsRepository
import com.example.kata.bank.service.infrastructure.operations.AmountDTO
import com.example.kata.bank.service.infrastructure.operations.OperationRequest
import com.example.kata.bank.service.infrastructure.operations.OperationService
import com.github.kittinunf.fuel.core.FuelManager
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.verifyZeroInteractions
import org.assertj.core.api.Assertions.assertThat
import org.junit.Ignore
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.platform.runner.JUnitPlatform
import org.junit.runner.RunWith
import org.mockito.Mockito

@RunWith(JUnitPlatform::class)
@Ignore("Problems with running two servers. See #1 - https://github.com/alvarogarcia7/bank-kata-kotlin/issues/1")
class ServiceIntegrationTest {

    object Mocks {
        @JvmStatic
        val operationService = Mockito.mock(OperationService::class.java)
    }

    @BeforeEach
    fun resetMocks() {
        Mockito.reset(Mocks.operationService)
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

        class MockOperationService : OperationService() {
            override fun deposit(account: Account?, depositRequest: OperationRequest.DepositRequest): Option<Id> {
                Mocks.operationService.deposit(account, depositRequest)
                return super.deposit(account, depositRequest)
            }
        }

        val accountRepository = AccountRepository()
        private val configuredApplication: () -> BankWebApplication = {
            BankWebApplication(
                    OperationsHandler(MockOperationService(), accountRepository),
                    AccountsHandler(accountRepository, XAPPlicationService(accountRepository, OperationsRepository())),
                    UsersHandler(UsersRepository())
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
                    verify(Mocks.operationService).deposit(account, OperationRequest.DepositRequest(AmountDTO.EUR("100.00"), description))
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

                    verifyZeroInteractions(Mocks.operationService)
                }
    }

    val http = HTTP

    private fun deposit(toAccount: Id, request: String) = HTTP.post("/accounts/${toAccount.value}/operations", request)
}
