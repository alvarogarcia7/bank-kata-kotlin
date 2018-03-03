package com.example.kata.bank.service.web

import com.example.kata.bank.service.infrastructure.Application
import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.core.Request
import com.github.kittinunf.fuel.core.Response
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.result.Result
import io.ktor.server.engine.ApplicationEngine
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.platform.runner.JUnitPlatform
import org.junit.runner.RunWith
import java.util.*
import java.util.concurrent.TimeUnit

@RunWith(JUnitPlatform::class)
class E2EServiceFeatureTest {

    private var server: ApplicationEngine? = null
    private var serverPort: Int? = null

    @BeforeEach
    fun setup() {
        while (true) {
            val current = Random().nextInt(3000) + 57000
            println("Trying to start on port $current...")
            try {
                server = Application.server(current)
                server?.start()
                serverPort = current
                break
            } catch (e: java.net.BindException) {
                e.printStackTrace()
                println("Port $current is already in use.")
            }
        }
    }

    @BeforeEach
    fun configurePort() {
        FuelManager.instance.basePath = "http://localhost:" + "8080"
    }

    @AfterEach
    fun stop() {
        server?.stop(3, 4, TimeUnit.SECONDS)
    }

    @org.junit.jupiter.api.Test
    fun testRequest() {
        val parameters = listOf(Pair("name", "me"))

        helloRequest(parameters)
                .let(this::request)
                .let { (response, result) ->
                    assertThat(response.statusCode).isEqualTo(200)
                    assertThat(result.value).isEqualToIgnoringCase("Hello me!")
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
