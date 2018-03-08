package com.example.kata.bank.service.delivery

import arrow.core.Option
import arrow.core.andThen
import arrow.core.getOrElse
import com.example.kata.bank.service.delivery.application.ApplicationEngine
import com.example.kata.bank.service.delivery.json.JSONMapper
import com.example.kata.bank.service.delivery.json.MyResponse
import com.example.kata.bank.service.domain.Id
import com.example.kata.bank.service.domain.Persisted
import com.example.kata.bank.service.domain.accounts.Account
import com.example.kata.bank.service.domain.accounts.AccountRepository
import com.example.kata.bank.service.domain.accounts.Clock
import com.example.kata.bank.service.domain.transactions.Amount
import com.example.kata.bank.service.domain.transactions.Transaction
import com.example.kata.bank.service.domain.users.UsersRepository
import com.example.kata.bank.service.infrastructure.AccountsService
import com.example.kata.bank.service.infrastructure.HelloService
import com.example.kata.bank.service.infrastructure.OperationsRepository
import com.example.kata.bank.service.infrastructure.accounts.AccountDTO
import com.example.kata.bank.service.infrastructure.operations.OperationService
import com.example.kata.bank.service.infrastructure.operations.TimeDTO
import com.example.kata.bank.service.infrastructure.operations.TransactionDTO
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.core.Request
import com.github.kittinunf.fuel.core.Response
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.fuel.httpPost
import com.github.kittinunf.result.Result
import org.assertj.core.api.AbstractAssert
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.assertj.core.api.SoftAssertions
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.platform.runner.JUnitPlatform
import org.junit.runner.RunWith
import java.time.LocalDateTime
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

        val operationsRepository = OperationsRepository()

        val accountRepository = AccountRepository()
        private val configuredApplication: () -> BankWebApplication = {
            BankWebApplication(
                    HelloService(),
                    OperationsHandler(
                            OperationService(),
                            accountRepository),
                    AccountsHandler(accountRepository, XAPPlicationService(accountRepository, operationsRepository)),
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
        val existingOperations = `operationsFor!`(accountId)

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
                }
        val newOperations = `operationsFor!`(accountId)
        this.bIsSupersetOfA(a = existingOperations, b = newOperations)
        assertThat(newOperations.size).isGreaterThan(existingOperations.size)
        TransactionAssert.assertThat(newOperations.last().value).isEqualToIgnoringDate(Transaction.Deposit(Amount.Companion.of("1234.56"), anyDate(), "rent for this month"))
    }

    private fun <T> forceGet(a: Option<T>): T {
        return a.getOrElse {
            fail("this element must be present")
            throw UnreachableCode()
        }

    }

    private fun operationsFor(accountId: Id): Option<List<Persisted<Transaction>>> {
        return AccountsService(accountRepository).operationsFor(accountId)
    }

    private val `operationsFor!` = this::operationsFor andThen this::forceGet


    private fun anyDate(): LocalDateTime {
        return LocalDateTime.now()
    }

    fun bIsSupersetOfA(a: List<Persisted<Transaction>>, b: List<Persisted<Transaction>>) {
        a.forEach {
            b.contains(it)
        }
    }

    class TransactionAssert(val actualT: Transaction) : AbstractAssert<TransactionAssert, Transaction>(actualT, TransactionAssert::class.java) {

        fun isEqualToIgnoringDate(transaction: Transaction): TransactionAssert {
            assertThat(transaction).isNotNull

            val softly = SoftAssertions()
            softly.assertThat(this.actualT.amount).isEqualTo(transaction.amount)
            softly.assertThat(this.actualT.description).isEqualTo(transaction.description)
            softly.assertAll()

            return this
        }

        companion object {

            // 3 - A fluent entry point to your specific assertion class, use it with static import.
            fun assertThat(actual: Transaction): TransactionAssert {
                return TransactionAssert(actual)
            }
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


    @Test
    fun `get a statement, without any filter`() {

        val accountId = Id(UUID.randomUUID().toString())
        accountRepository.save(Persisted.`for`(aNewAccount(), accountId))
        accountRepository.findBy(accountId)
                .map {
                    it.value.deposit(Amount.Companion.of("100"), "rent, part 1")
                    it.value.deposit(Amount.Companion.of("200"), "rent, part 2")
                }
        createStatement(accountId.value, StatementRequestDTO("statement"))
                .let(this::request)
                .let { (response, result) ->
                    assertThat(response.statusCode).isEqualTo(200)
                    println(result.value)
                    val objectMapper = JSONMapper.aNew()
                    val x = objectMapper.readValue<MyResponse<String>>(result.value)
                    val statementId = x.links.find { it.rel == "self" }?.href?.split("/")?.last()!!
                    println("expecting statmentid = $statementId")
                    operationsRepository.findAll().forEach {
                        println("Found operation: $it")
                    }
                    assertThat(operationsRepository.findBy(Id(statementId)).isDefined()).isTrue()


//                    val deposits = x.response.transactions.map {
//                        val it1 = it as Transaction.Deposit
//                        it1.copy(time = fixedTime)
//                    }.map { Mapper().toDTO(it) }
//                    assertThat(deposits).contains(
//                            TransactionDTO(AmountDTO.EUR("100"), "rent, part 1", fixedTimeDTO),
//                            TransactionDTO(AmountDTO.EUR("200"), "rent, part 2", fixedTimeDTO))
                }
    }

    @Test
    fun `try to create an unsupported type of request`() {

        val accountId = Id(UUID.randomUUID().toString())
        accountRepository.save(Persisted.`for`(aNewAccount(), accountId))
        createStatement(accountId.value, StatementRequestDTO("unsupported"))
                .let { this.assertFailedRequest(it, this::assertError) }
                .let { (response, _) ->
                    assertThat(response.statusCode).isEqualTo(400)
                    val objectMapper = JSONMapper.aNew()
                    val errors = objectMapper.readValue<MyResponse<ErrorsDTO>>(String(response.data).replace("\\n".toRegex(), ""))
                    assertThat(errors.response.messages).contains("This operation is not supported for now")
                }
    }

    private fun aNewAccount() = aNewAccount("savings account #" + Random().nextInt(10))

    private fun aNewAccount(accountName: String) = Account(Clock.aNew(), accountName)

    private fun depositRequest(accountId: Id, jsonPayload: String): Request {
        return post("accounts/${accountId.value}/operations", jsonPayload)
    }

    private fun createStatement(value: String, request: StatementRequestDTO): Request {
        return post("/accounts/$value", JSONMapper.aNew().writeValueAsString(request))
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
            return assertSuccess(response, result)
        } catch (e: Exception) {
            e.printStackTrace()
            fail("exception: " + e.message)
            throw RuntimeException() // unreachable code
        }
    }

    private fun <T> assertFailedRequest(
            request: Request,
            x: (Response, Result<String, FuelError>) -> T): T {
        try {
            val (_, response, result) = request.responseString()
            return x.invoke(response, result)
        } catch (e: Exception) {
            e.printStackTrace()
            fail("exception: " + e.message)
            throw RuntimeException() // unreachable code
        }
    }

    @Throws(UnreachableCode::class)
    private fun assertSuccess(response: Response, result: Result<String, FuelError>): Pair<Response, Result.Success<String, FuelError>> {
        return when (result) {
            is Result.Success -> {
                val pair = Pair(response, result)
                pair
            }
            is Result.Failure -> {
                fail("expected a Result.success: " + result.error)
                throw UnreachableCode()
            }
            else -> {
                fail("expected a Result.success: " + result.javaClass)
                throw UnreachableCode()
            }
        }
    }

    @Throws(UnreachableCode::class)
    private fun assertError(response: Response, result: Result<String, FuelError>): Pair<Response, Result.Failure<String, FuelError>> {
        return when (result) {
            is Result.Success -> {
                fail("expected a Result.error: " + result.value)
                throw UnreachableCode()
            }
            is Result.Failure -> {
                return Pair(response, result)
            }
            else -> {
                fail("expected a Result.error: " + result.javaClass)
                throw UnreachableCode()
            }
        }
    }

    private fun helloRequest(parameters: List<Pair<String, String>>) = "/".httpGet(parameters)

    val fixedTime = LocalDateTime.of(2018, 10, 12, 23, 59)
    val fixedTimeDTO = TimeDTO("2018-10-12 23:59:00", "2018-10-12T23:59:00")
}

/**
 * Class to satisfy the compiler.
 *
 * As a programmer I'm sure that the previous code will throw a runtime exception to interrupt the execution,
 * but the compile cannot realise about this, as they're not detectable until execution time
 */
class UnreachableCode : Throwable()

