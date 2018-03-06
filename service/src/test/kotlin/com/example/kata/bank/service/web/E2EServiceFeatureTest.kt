package com.example.kata.bank.service.web

import com.example.kata.bank.service.infrastructure.HelloService
import com.example.kata.bank.service.infrastructure.JSONMapper
import com.example.kata.bank.service.infrastructure.application.ApplicationEngine
import com.example.kata.bank.service.infrastructure.application.BankWebApplication
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

        private val configuredApplication: () -> BankWebApplication = { BankWebApplication(HelloService(), OperationService()) }
    }


    @Test
    fun `deposit - a correct request`() {
        depositRequest(1234, """
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
                    assertThat(x.payload).isEqualTo("")
                }
    }

    private fun depositRequest(userId: Int, jsonPayload: String): Request {
        return "users/$userId/operations".httpPost().header("Content-Type" to "application/json").body(jsonPayload, Charsets.UTF_8)
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

