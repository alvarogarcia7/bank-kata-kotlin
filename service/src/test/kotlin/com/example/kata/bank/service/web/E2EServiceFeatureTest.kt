package com.example.kata.bank.service.web

import com.example.kata.bank.service.infrastructure.BankWebApplication
import com.example.kata.bank.service.infrastructure.HelloService
import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.core.Request
import com.github.kittinunf.fuel.core.Response
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.result.Result
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.platform.runner.JUnitPlatform
import org.junit.runner.RunWith
import java.net.BindException
import java.util.*

@RunWith(JUnitPlatform::class)
class E2EServiceFeatureTest {

    private var serverPort: Int? = null
    private var application: BankWebApplication? = null

    @BeforeEach
    fun setup() {
        val (application, serverPort) = startAtRandomPort()
        this.serverPort = serverPort
        this.application = application
    }

    private fun startAtRandomPort(): Pair<BankWebApplication, Int> {
        val randomGenerator = Random()
        while (true) {
            val currentPort = randomGenerator.nextInt(3000) + 57000
            println("Trying to start on port $currentPort...")
            try {
                val application = configuredApplication().start(currentPort)
                return Pair(application, currentPort)
            } catch (e: BindException) {
                e.printStackTrace()
                println("Port $currentPort is already in use.")
            }
        }
    }

    private fun configuredApplication(): BankWebApplication = BankWebApplication(HelloService())

    @BeforeEach
    fun configurePort() {
        FuelManager.instance.basePath = "http://localhost:" + serverPort
    }

    @AfterEach
    fun stop() {
        application?.stop()
    }

    @Test
    fun `salute - all features`() {

        helloRequest(listOf(Pair("name", "me")))
                .let(this::request)
                .let { (response, result) ->
                    println(result)
                    assertThat(response.statusCode).isEqualTo(200)
                    assertThat(result.value).isEqualToIgnoringCase("Hello me!")
                }


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
