package com.example.kata.bank.service.delivery

import com.example.kata.bank.service.delivery.application.ApplicationEngine
import com.example.kata.bank.service.domain.accounts.AccountRepository
import com.example.kata.bank.service.domain.users.UsersRepository
import com.example.kata.bank.service.infrastructure.HelloRequest
import com.example.kata.bank.service.infrastructure.HelloService
import com.example.kata.bank.service.infrastructure.operations.OperationService
import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.core.Request
import com.github.kittinunf.fuel.core.Response
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.result.Result
import com.nhaarman.mockito_kotlin.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
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
        val helloService = Mockito.mock(HelloService::class.java)
    }

    @BeforeEach
    fun resetMocks() {
        Mockito.reset(Mocks.helloService)
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

        class MockHelloService : HelloService() {
            override fun salute(request: HelloRequest): String {
                Mocks.helloService.salute(request)
                return super.salute(request)
            }
        }

        private val configuredApplication: () -> BankWebApplication = {
            val accountRepository = AccountRepository()
            BankWebApplication(
                    MockHelloService(),
                    OperationsHandler(OperationService(), accountRepository),
                    AccountsHandler(accountRepository),
                    UsersHandler(UsersRepository())
            )
        }
    }


    @Test
    fun `salute - with a name`() {

        helloRequest(listOf(Pair("name", "me")))
                .let(this::request)
                .let { (response, result) ->
                    println(result)
                    assertThat(response.statusCode).isEqualTo(200)

                    verify(Mocks.helloService).salute(HelloRequest("me"))
                }
    }

    @Test
    fun `salute - no name`() {

        helloRequest(emptyList())
                .let(this::request)
                .let { (response, result) ->
                    println(result)
                    assertThat(response.statusCode).isEqualTo(200)

                    verify(Mocks.helloService).salute(HelloRequest(null))
                }
    }

    private fun request(request: Request): Pair<Response, Result.Success<String, FuelError>> {
        try {
            val (_, response, result) = request.responseString()

            when (result) {
                is Result.Success -> {
                    return Pair(response, result)
                }
                is Result.Failure -> {
                    fail("expected a Result.success: " + result.error)
                    throw RuntimeException() // unreachable code
                }
                else -> {
                    fail("expected a Result.success: " + result.javaClass)
                    throw RuntimeException() // unreachable code
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            fail("exception: " + e.message)
            throw RuntimeException() // unreachable code
        }
    }

    private fun helloRequest(parameters: List<Pair<String, String>>) = "/".httpGet(parameters)
}
