package com.example.kata.bank.service.delivery

import com.example.kata.bank.service.delivery.application.ApplicationEngine
import com.example.kata.bank.service.delivery.json.JSONMapper
import com.example.kata.bank.service.delivery.json.MyResponse
import com.example.kata.bank.service.domain.Account
import com.example.kata.bank.service.domain.Clock
import com.example.kata.bank.service.domain.Persisted
import com.example.kata.bank.service.infrastructure.HelloService
import com.example.kata.bank.service.infrastructure.operations.OperationService
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.core.Request
import com.github.kittinunf.fuel.core.Response
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.fuel.httpPost
import com.github.kittinunf.result.Result
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.platform.runner.JUnitPlatform
import org.junit.runner.RunWith
import java.util.*

@RunWith(JUnitPlatform::class)
class E2EServiceFeatureTest {

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

        val accountRepository = AccountRepository()
        private val configuredApplication: () -> BankWebApplication = {
            BankWebApplication(
                    HelloService(),
                    OperationsHandler(
                            OperationService(),
                            accountRepository),
                    AccountsHandler(accountRepository),
                    UsersHandler(UsersRepository()))
        }
    }


    @Test
    fun `list accounts`() {
        accountRepository.save(Persisted.`for`(Account(Clock.aNew()), UUID.randomUUID()))
        accountRepository.save(Persisted.`for`(Account(Clock.aNew()), UUID.randomUUID()))
        accountRepository.save(Persisted.`for`(Account(Clock.aNew()), UUID.randomUUID()))


        get("/accounts")
                .let(this::request)
                .let { (response, result) ->
                    assertThat(response.statusCode).isEqualTo(200)
                    println(result.value)
                    ""
                }
    }


    @Test
    fun `deposit - a correct request`() {

        val accountId = UUID.randomUUID()
        accountRepository.save(Persisted.`for`(Account(Clock.aNew()), accountId))
        depositRequest(accountId, """
        {
            "type": "deposit",
            "amount": {
            "value": "1234.56",
            "currency": "EUR"
        },
            "description": "rent for this month"
        }
        """).let(this::request)
                .let { (response, result) ->
                    assertThat(response.statusCode).isEqualTo(200)
                    val objectMapper = JSONMapper.aNew()
                    val x = objectMapper.readValue<MyResponse<String>>(result.value)
                    println(x)
                    assertThat(x.links).hasSize(1)
                    assertThat(x.links).filteredOn { it.rel == "list" }.isNotEmpty()
                    assertThat(x.response).isEqualTo("")
                    val url = x.links.find { it.rel == "list" }?.href
                    get(url!!)
                            .let(this::request)
                            .let { (response, result) ->
                                assertThat(response.statusCode).isEqualTo(200)
                                println(result.value)
                            }
                    ""
                }
    }

    private fun depositRequest(accountId: UUID, jsonPayload: String): Request {
        return "accounts/$accountId/operations".httpPost().header("Content-Type" to "application/json").body(jsonPayload, Charsets.UTF_8)
    }

    private fun get(url: String): Request {
        return url.httpGet()
    }

    @Test
    fun `salute - with a name`() {

        helloRequest(listOf(Pair("name", "me")))
                .let(this::request)
                .let { (response, result) ->
                    println(result)
                    assertThat(response.statusCode).isEqualTo(200)
                    assertThat(result.value).isEqualToIgnoringCase("Hello me!")
                }
    }

    @Test
    fun `salute - no name`() {

        helloRequest(emptyList())
                .let(this::request)
                .let { (response, result) ->
                    println(result)
                    assertThat(response.statusCode).isEqualTo(200)
                    assertThat(result.value).isEqualToIgnoringCase("Hello, world!")
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

