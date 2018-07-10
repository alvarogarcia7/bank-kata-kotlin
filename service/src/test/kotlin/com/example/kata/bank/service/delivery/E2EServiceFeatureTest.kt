package com.example.kata.bank.service.delivery

import arrow.core.Option
import arrow.core.andThen
import arrow.core.getOrElse
import com.example.kata.bank.service.ApplicationBooter
import com.example.kata.bank.service.HTTP
import com.example.kata.bank.service.UnreachableCode
import com.example.kata.bank.service.delivery.`in`.StatementRequestDTO
import com.example.kata.bank.service.delivery.application.ApplicationEngine
import com.example.kata.bank.service.delivery.handlers.AccountsHandler
import com.example.kata.bank.service.delivery.handlers.OperationsHandler
import com.example.kata.bank.service.delivery.handlers.UsersHandler
import com.example.kata.bank.service.delivery.json.MyResponse
import com.example.kata.bank.service.delivery.out.ErrorsDTO
import com.example.kata.bank.service.delivery.out.StatementOutDTO
import com.example.kata.bank.service.domain.AccountRequest
import com.example.kata.bank.service.domain.Id
import com.example.kata.bank.service.domain.Persisted
import com.example.kata.bank.service.domain.accounts.Account
import com.example.kata.bank.service.domain.accounts.Clock
import com.example.kata.bank.service.domain.transactions.Amount
import com.example.kata.bank.service.domain.transactions.Transaction
import com.example.kata.bank.service.domain.transactions.Tx
import com.example.kata.bank.service.infrastructure.accounts.AccountRestrictedRepository
import com.example.kata.bank.service.infrastructure.accounts.AccountsService
import com.example.kata.bank.service.infrastructure.accounts.out.AccountDTO
import com.example.kata.bank.service.infrastructure.operations.AmountDTO
import com.example.kata.bank.service.infrastructure.operations.OperationsRepository
import com.example.kata.bank.service.infrastructure.operations.out.StatementLineDTO
import com.example.kata.bank.service.infrastructure.operations.out.TimeDTO
import com.example.kata.bank.service.infrastructure.operations.out.TransactionDTO
import com.example.kata.bank.service.infrastructure.users.UsersSimpleRepository
import com.example.kata.bank.service.usecases.accounts.DepositUseCase
import com.example.kata.bank.service.usecases.accounts.OpenAccountUseCase
import com.example.kata.bank.service.usecases.accounts.TransferUseCase
import com.example.kata.bank.service.usecases.statements.StatementCreationUseCase
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.core.Request
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

        val accountRepository = AccountRestrictedRepository.aNew()
        private val configuredApplication: () -> BankWebApplication = {
            BankWebApplication(
                    OperationsHandler(
                            accountRepository,
                            TransferUseCase(accountRepository),
                            DepositUseCase(accountRepository),
                            operationsRepository),
                    AccountsHandler(accountRepository, StatementCreationUseCase(operationsRepository), OpenAccountUseCase(accountRepository)),
                    UsersHandler(UsersSimpleRepository()))
        }
    }


    @Test
    fun `create multiple accounts`() {
        val existingAccountSize = readExistingAccounts().size

        openAccount("maria").let(http::request)
        openAccount("maria").let(http::request)
        openAccount("maria").let(http::request)

        assertThat(readExistingAccounts().size).isEqualTo(existingAccountSize + 3)
    }

    private fun readExistingAccounts(): List<MyResponse<AccountDTO>> {
        return (HTTP::get)("/accounts")
                .let(http::request)
                .also { (response, _) ->
                    assertThat(response.statusCode).isEqualTo(200)
                }.let { (_, result) -> http.mapper.readValue<List<MyResponse<AccountDTO>>>(result.value) }
    }

    @Test
    fun `an account has a link to self`() {
        openAccount("maria").let(http::request)

        readExistingAccounts().last()
                .let { it ->
                    it.links.find { it.rel == "self" }?.resource("accounts")!!
                }
    }

    @Test
    fun `detail for an account`() {
        val accountId = Id.random()
        accountRepository.save(Persisted.`for`(aNewAccount("pepe", Account.Number.of("00-00-00-01")), accountId))

        (HTTP::get)(account(accountId))
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
                    val response = http.mapper.readValue<MyResponse<Unit>>(result.value)
                    println(response)
                    assertThat(response.links).hasSize(1)
                    assertThat(response.links).filteredOn { it.rel == "list" }.isNotEmpty()
                }
        val newOperations = `operationsFor!`(accountId)
        this.bIsSupersetOfA(a = existingOperations, b = newOperations)
        assertThat(newOperations.size).isGreaterThan(existingOperations.size)
        TransactionAssert.assertThat(newOperations.last().value).isEqualToIgnoringDate(Transaction.Deposit(Tx(Amount.of("1234.56"), anyDate(), "rent for this month")))
    }


    @Test
    fun `wire transfer - a correct request`() {

        val accountId = Id.random()
        accountRepository.save(Persisted.`for`(aNewAccount(), accountId))
        val existingOperations = `operationsFor!`(accountId)
        val destinationId = Id.random()
        accountRepository.save(Persisted.`for`(aNewAccount(accountNumber = Account.Number.of("11")), destinationId))

        depositRequest(accountId, """
{
    "type": "transfer",
    "amount": {
      "value": "1234.56",
      "currency": "EUR"
	},
	"destination":{
		"number":"11",
		"owner": "Maria"
	},
    "description": "rent for this month"
}
""".trimIndent()).let(http::request)
                .let { (response, result) ->
                    assertThat(response.statusCode).isEqualTo(200)
                    val response = http.mapper.readValue<MyResponse<Unit>>(result.value)
                    println(response)
                    assertThat(response.links).hasSize(1)
                    assertThat(response.links).filteredOn { it.rel == "list" }.isNotEmpty()
                }
        val newOperations = `operationsFor!`(accountId)
        this.bIsSupersetOfA(a = existingOperations, b = newOperations)
        assertThat(newOperations.size).isGreaterThan(existingOperations.size)
//        TransactionAssert.assertThat(newOperations.last().value).isEqualToIgnoringDate(Transaction.Transfer.Emitted(Tx(Amount.of("1234.56"), anyDate(), "rent for this month"),
//                Transaction.Transfer.Completed(accountId, destinationId)))
//        TransactionAssert.assertThat(`operationsFor!`(destinationId).last().value).isEqualToIgnoringDate(Transaction.Transfer.Received(Tx(Amount.of("1234.56"), anyDate(),
//                "rent for this month"),
//                Transaction.Transfer.Completed(accountId, destinationId)))
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
            softly.assertThat(this.actualT.tx.amount).isEqualTo(transaction.tx.amount)
            softly.assertThat(this.actualT.tx.description).isEqualTo(transaction.tx.description)
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
        (HTTP::get)(operations(accountId))
                .let(http::request)
                .let { (response, result) ->
                    assertThat(response.statusCode).isEqualTo(200)
                    println(result.value)
                    val response = http.mapper.readValue<List<MyResponse<TransactionDTO>>>(result.value)
                    assertThat(response).hasSize(2)
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
                    val response = http.mapper.readValue<MyResponse<String>>(result.value)
                    val statementPair = response.links.find { it.rel == "self" }?.resource("statements")!!
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
                    StatementCreationUseCase(operationsRepository).createStatement(it.value, AccountRequest.StatementRequest.all())
                }.getOrElse { throw UnreachableCode() }


        (HTTP::get)(statement(accountId, statementId))
                .let(http::request)
                .let { (response, result) ->
                    assertThat(response.statusCode).isEqualTo(200)
                    result
                }.let(readAsMyResponseStatementOutDTO)
                .let { x ->
                    val deposits = setTime(x, fixedTimeDTO)
                    assertThat(deposits).contains(
                            StatementLineDTO(AmountDTO.EUR("100.00"), "rent, part 1", fixedTimeDTO, "deposit", AmountDTO.EUR("100.00")),
                            StatementLineDTO(AmountDTO.EUR("200.00"), "rent, part 2", fixedTimeDTO, "deposit", AmountDTO.EUR("300.00")))
                }
    }

    val readAsMyResponseStatementOutDTO: (Result.Success<String, FuelError>) -> MyResponse<StatementOutDTO> = { http.mapper.readValue(it.value) }


    private fun statement(accountId: Id, statementId: Id) =
            "/accounts/${accountId.value}/statements/${statementId.value}"

    private fun setTime(coll: MyResponse<StatementOutDTO>, value: TimeDTO): List<StatementLineDTO> {
        return coll.response.statementLines.map {
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

    private fun operations(accountId: Id) = "/accounts/${accountId.value}/operations"

    private fun aNewAccount(accountNumber: Account.Number = Account.Number.of(Id.random().value)) = aNewAccount("savings account #" + Random().nextInt(10), accountNumber)

    private fun account(accountId: Id) = "/accounts/${accountId.value}"

    private fun aNewAccount(accountName: String, accountNumber: Account.Number) = Account(Clock.aNew(), accountName, number = accountNumber)

    private fun depositRequest(accountId: Id, jsonPayload: String): Request {
        return http.post("accounts/${accountId.value}/operations", jsonPayload)
    }

    private fun createStatement(value: String, request: StatementRequestDTO): Request {
        return http.post("/accounts/$value", request)
    }

    private fun transactionsFor(accountId: Id) = accountRepository.findBy(accountId).map { it.value.findAll().map { it.value } }

    val fixedTimeDTO = TimeDTO("2018-10-12 23:59:00", "2018-10-12T23:59:00")
}

