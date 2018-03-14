package com.example.kata.bank.service.delivery

import arrow.core.Option
import arrow.core.andThen
import arrow.core.getOrElse
import com.example.kata.bank.service.ApplicationBooter
import com.example.kata.bank.service.HTTP
import com.example.kata.bank.service.delivery.application.ApplicationEngine
import com.example.kata.bank.service.delivery.json.MyResponse
import com.example.kata.bank.service.domain.AccountRequest
import com.example.kata.bank.service.domain.Id
import com.example.kata.bank.service.domain.Persisted
import com.example.kata.bank.service.domain.accounts.Account
import com.example.kata.bank.service.domain.accounts.AccountRepository
import com.example.kata.bank.service.domain.accounts.Clock
import com.example.kata.bank.service.domain.transactions.Amount
import com.example.kata.bank.service.domain.transactions.Transaction
import com.example.kata.bank.service.domain.users.UsersRepository
import com.example.kata.bank.service.infrastructure.AccountsService
import com.example.kata.bank.service.infrastructure.OperationsRepository
import com.example.kata.bank.service.infrastructure.accounts.AccountDTO
import com.example.kata.bank.service.infrastructure.operations.AmountDTO
import com.example.kata.bank.service.infrastructure.operations.OperationService
import com.example.kata.bank.service.infrastructure.operations.TimeDTO
import com.example.kata.bank.service.infrastructure.operations.TransactionDTO
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.core.Request
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

        val accountRepository = AccountRepository()
        private val configuredApplication: () -> BankWebApplication = {
            BankWebApplication(
                    OperationsHandler(
                            OperationService(),
                            accountRepository),
                    AccountsHandler(accountRepository, XAPPlicationService(accountRepository, operationsRepository)),
                    UsersHandler(UsersRepository()))
        }
    }


    @Test
    fun `list accounts`() {
        accountRepository.save(Persisted.`for`(aNewAccount(), Id.random()))
        accountRepository.save(Persisted.`for`(aNewAccount(), Id.random()))
        accountRepository.save(Persisted.`for`(aNewAccount(), Id.random()))


        (HTTP::get)("/accounts")
                .let(http::request)
                .let { (response, result) ->
                    assertThat(response.statusCode).isEqualTo(200)
                    println(result.value)
                    ""
                }
    }

    @Test
    fun `create accounts`() {

        HTTP.post("/accounts", "{\"name\": \"postperson savings account\"}")
                .let(http::request)
                .let { (response, result) ->
                    assertThat(response.statusCode).isEqualTo(200)
                    println(result.value)
                    val account = http.mapper.readValue<MyResponse<AccountDTO>>(result.value)
                    val statementPair = account.links.find { it.rel == "self" }?.resource("accounts")!!
                    statementPair.map { (resource, idValue) ->
                        assertThat(accountRepository.findBy(Id.of(idValue)).isDefined()).isTrue()
                    }
                }

    }

    @Test
    fun `detail for an account`() {
        val accountId = Id.random()
        accountRepository.save(Persisted.`for`(aNewAccount("pepe"), accountId))

        (HTTP::get)("/accounts/${accountId.value}")
                .let(http::request)
                .let { (response, result) ->
                    assertThat(response.statusCode).isEqualTo(200)
                    println(result.value)
                    val account = http.mapper.readValue<MyResponse<AccountDTO>>(result.value)
                    assertThat(account.response.name).isEqualTo("pepe")
                }
    }

    @Test
    fun `create account`() {
        val accountName = "savings aNewAccount for maria"
        openAccount(name = accountName)
                .let(http::request)
                .let { (response, result) ->
                    println(result.value)
                    assertThat(response.statusCode).isEqualTo(200)
                    val r = http.mapper.readValue<MyResponse<AccountDTO>>(result.value)
                    assertThat(r.response.name).isEqualTo(accountName)
                }
    }

    private fun openAccount(name: String): Request {
        return (HTTP::post)("accounts", """{"name": "$name"}""")
    }


    @Test
    fun `deposit - a correct request`() {

        val accountId = Id.random()
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
        """).let(http::request)
                .let { (response, result) ->
                    assertThat(response.statusCode).isEqualTo(200)
                    val x = http.mapper.readValue<MyResponse<Unit>>(result.value)
                    println(x)
                    assertThat(x.links).hasSize(1)
                    assertThat(x.links).filteredOn { it.rel == "list" }.isNotEmpty()
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

        val accountId = Id.random()
        accountRepository.save(Persisted.`for`(aNewAccount(), accountId))
        accountRepository.findBy(accountId)
                .map {
                    it.value.deposit(Amount.Companion.of("100"), "rent, part 1")
                    it.value.deposit(Amount.Companion.of("200"), "rent, part 2")
                }
        (HTTP::get)("/accounts/${accountId.value}/operations")
                .let(http::request)
                .let { (response, result) ->
                    assertThat(response.statusCode).isEqualTo(200)
                    println(result.value)
                    val x = http.mapper.readValue<List<MyResponse<TransactionDTO>>>(result.value)
                    assertThat(x).hasSize(2)
                }
    }

    @Test
    fun `create a statement, without any filter - creates a new Cost`() {

        val accountId = Id.random()
        accountRepository.save(Persisted.`for`(aNewAccount(), accountId))
        accountRepository.findBy(accountId)
                .map {
                    it.value.deposit(Amount.Companion.of("100"), "rent, part 1")
                    it.value.deposit(Amount.Companion.of("200"), "rent, part 2")
                }
        val previousCosts = forceGet(transactionsFor(accountId)).filter { it is Transaction.Cost }.size

        createStatement(accountId.value, StatementRequestDTO("statement"))
                .let(http::request)
                .let { (response, result) ->
                    assertThat(response.statusCode).isEqualTo(200)
                    println(result.value)
                    val x = http.mapper.readValue<MyResponse<String>>(result.value)
                    val statementPair = x.links.find { it.rel == "self" }?.resource("statements")!!
                    statementPair.map {
                        val (_, statementId) = it
                        assertThat(operationsRepository.findBy(Id.of(statementId)).isDefined()).isTrue()
                        val newCosts = forceGet(transactionsFor(accountId)).filter { it is Transaction.Cost }.size
                        assertThat(newCosts).isEqualTo(previousCosts + 1)
                    }
                }
    }

    @Test
    fun `fetch a statement`() {

        val accountId = Id.random()
        accountRepository.save(Persisted.`for`(aNewAccount(), accountId))
        val statementId = accountRepository.findBy(accountId)
                .map {
                    it.value.deposit(Amount.Companion.of("100"), "rent, part 1")
                    it.value.deposit(Amount.Companion.of("200"), "rent, part 2")
                    XAPPlicationService(accountRepository, operationsRepository).createAndSaveOperation(it.value, AccountRequest.StatementRequest.all())
                }.getOrElse { throw UnreachableCode() }

        (HTTP::get)("/accounts/${accountId.value}/statements/${statementId.value}")
                .let(http::request)
                .let { (response, result) ->
                    assertThat(response.statusCode).isEqualTo(200)
                    println(result.value)
                    val x = http.mapper.readValue<MyResponse<StatementOutDTO>>(result.value)
                    val deposits = setTime(x, fixedTimeDTO)
                    assertThat(deposits).contains(
                            TransactionDTO(AmountDTO.EUR("100.00"), "rent, part 1", fixedTimeDTO, "deposit"),
                            TransactionDTO(AmountDTO.EUR("200.00"), "rent, part 2", fixedTimeDTO, "deposit"))
                }
    }

    private fun setTime(coll: MyResponse<StatementOutDTO>, value: TimeDTO): List<TransactionDTO> {
        return coll.response.transactions.map {
            it.copy(time = value)
        }
    }

    @Test
    fun `try to create an unsupported type of request`() {

        val accountId = Id.random()
        accountRepository.save(Persisted.`for`(aNewAccount(), accountId))
        createStatement(accountId.value, StatementRequestDTO("unsupported"))
                .let { http.assertFailedRequest(it, http::assertError) }
                .let { (response, _) ->
                    assertThat(response.statusCode).isEqualTo(400)
                    val errors = http.mapper.readValue<MyResponse<ErrorsDTO>>(String(response.data).replace("\\n".toRegex(), ""))
                    assertThat(errors.response.messages).contains("This operation is not supported for now")
                }
    }

    private fun aNewAccount() = aNewAccount("savings account #" + Random().nextInt(10))

    private fun aNewAccount(accountName: String) = Account(Clock.aNew(), accountName)

    private fun depositRequest(accountId: Id, jsonPayload: String): Request {
        return http.post("accounts/${accountId.value}/operations", jsonPayload)
    }

    private fun createStatement(value: String, request: StatementRequestDTO): Request {
        return http.post("/accounts/$value", request)
    }

    private fun transactionsFor(accountId: Id) = accountRepository.findBy(accountId).map { it.value.findAll().map { it.value } }

    val fixedTimeDTO = TimeDTO("2018-10-12 23:59:00", "2018-10-12T23:59:00")
}

/**
 * Class to satisfy the compiler.
 *
 * As a programmer I'm sure that the previous code will throw a runtime exception to interrupt the execution,
 * but the compile cannot realise about this, as they're not detectable until execution time
 */
class UnreachableCode : Throwable()

