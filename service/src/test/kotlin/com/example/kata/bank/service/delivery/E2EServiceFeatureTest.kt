package com.example.kata.bank.service.delivery

import com.example.kata.bank.service.delivery.application.ApplicationEngine
import com.example.kata.bank.service.delivery.json.JSONMapper
import com.example.kata.bank.service.delivery.json.MyResponse
import com.example.kata.bank.service.domain.Id
import com.example.kata.bank.service.domain.Persisted
import com.example.kata.bank.service.domain.accounts.Account
import com.example.kata.bank.service.domain.accounts.AccountRepository
import com.example.kata.bank.service.domain.accounts.Clock
import com.example.kata.bank.service.domain.transactions.Amount
import com.example.kata.bank.service.domain.users.UsersRepository
import com.example.kata.bank.service.infrastructure.HelloService
import com.example.kata.bank.service.infrastructure.accounts.AccountDTO
import com.example.kata.bank.service.infrastructure.operations.OperationService
import com.example.kata.bank.service.infrastructure.operations.TransactionDTO
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
        accountRepository.save(Persisted.`for`(aNewAccount(), Id(UUID.randomUUID().toString())))
        accountRepository.save(Persisted.`for`(aNewAccount(), Id(UUID.randomUUID().toString())))
        accountRepository.save(Persisted.`for`(aNewAccount(), Id(UUID.randomUUID().toString())))


        get("/accounts")
                .let(this::request)
                .let { (response, result) ->
                    assertThat(response.statusCode).isEqualTo(200)
                    println(result.value)
                    ""
                }
    }

    @Test
    fun `detail for an account`() {
        val accountId = Id(UUID.randomUUID().toString())
        accountRepository.save(Persisted.`for`(aNewAccount("pepe"), accountId))

        get("/accounts/${accountId.value}")
                .let(this::request)
                .let { (response, result) ->
                    assertThat(response.statusCode).isEqualTo(200)
                    println(result.value)
                    val account = JSONMapper.aNew().readValue<MyResponse<AccountDTO>>(result.value)
                    assertThat(account.response.name).isEqualTo("pepe")
                }
    }

    @Test
    fun `create account`() {
        val accountName = "savings aNewAccount for maria"
        openAccount(name = accountName)
                .let(this::request)
                .let { (response, result) ->
                    println(result.value)
                    assertThat(response.statusCode).isEqualTo(200)
                    val r = JSONMapper.aNew().readValue<MyResponse<AccountDTO>>(result.value)
                    assertThat(r.response.name).isEqualTo(accountName)
                }
    }

    private fun openAccount(name: String): Request {
        return post("accounts", """
            {"name": "$name"}
            """)
    }


    @Test
    fun `deposit - a correct request`() {

        val accountId = Id(UUID.randomUUID().toString())
        accountRepository.save(Persisted.`for`(aNewAccount(), accountId))
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

    @Test
    fun `list the operations`() {

        val accountId = Id(UUID.randomUUID().toString())
        accountRepository.save(Persisted.`for`(aNewAccount(), accountId))
        accountRepository.findBy(accountId)
                .map {
                    it.value.deposit(Amount.Companion.of("100"), "rent, part 1")
                    it.value.deposit(Amount.Companion.of("200"), "rent, part 2")
                }
        get("/accounts/${accountId.value}/operations")
                .let(this::request)
                .let { (response, result) ->
                    assertThat(response.statusCode).isEqualTo(200)
                    println(result.value)
                    val objectMapper = JSONMapper.aNew()
                    val x = objectMapper.readValue<List<MyResponse<TransactionDTO>>>(result.value)
                    assertThat(x).hasSize(2)
                }
    }

    private fun aNewAccount() = aNewAccount("savings account #" + Random().nextInt(10))

    private fun aNewAccount(accountName: String) = Account(Clock.aNew(), accountName)

    private fun depositRequest(accountId: Id, jsonPayload: String): Request {
        return post("accounts/${accountId.value}/operations", jsonPayload)
    }

    private fun post(url: String, body: String) = url.httpPost().header("Content-Type" to "application/json").body(body, Charsets.UTF_8)

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

