package com.example.kata.bank.service.domain.accounts

import arrow.core.None
import arrow.core.Option
import arrow.core.Some
import com.example.kata.bank.service.domain.AccountRequest
import com.example.kata.bank.service.domain.FakeClock
import com.example.kata.bank.service.domain.Id
import com.example.kata.bank.service.domain.Persisted
import com.example.kata.bank.service.domain.transactions.Amount
import com.example.kata.bank.service.domain.transactions.Transaction
import com.example.kata.bank.service.domain.transactions.Transaction.Transfer.*
import com.example.kata.bank.service.domain.transactions.Tx
import com.nhaarman.mockito_kotlin.doAnswer
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.SoftAssertions
import org.junit.Test

abstract class AccountShould {
    @Test
    fun `create a filtered statement, just the Deposits`() {
        val account = AccountBuilder.aNew(this::account).movements().build()

        val statement = account.createStatement(AccountRequest.StatementRequest.filter { it is Transaction.Deposit })

        assertThat(statement.lines).hasSize(3) //1 (initial) + 2 (above)
    }

    @Test
    fun `create a filtered statement, just the Withdrawals`() {
        val account = AccountBuilder.aNew(this::account).movements().build()

        val statement = account.createStatement(AccountRequest.StatementRequest.filter { it is Transaction.Withdrawal })

        assertThat(statement.lines).hasSize(2) //1 (initial) + 1 (above)
    }

    @Test
    fun `create a filtered statement --that produces no results-- does not cost the personal user a dime`() {
        val account = AccountBuilder.aNew(this::account).movements().build()

        account.createStatement(AccountRequest.StatementRequest.filter { its -> false })

        assertThat(costsFor(account)).hasSize(0)
    }

    @Test
    fun `can withdraw even if it would empty the account `() {
        val account = AccountBuilder.aNew(this::account).build()
        account.deposit(Amount.of("100"), "initial deposit")
        assertThat(account.findAll()).hasSize(1)

        val result = account.withdraw(Amount.of("100"), "overdraft")

        assertThat(result.isRight()).isTrue()
        assertThat(account.findAll()).hasSize(2)
    }

    @Test
    fun `can perform multiple withdraws even if it would empty the account`() {
        val account = AccountBuilder.aNew(this::account).build()
        account.deposit(Amount.of("100"), "initial deposit")
        account.withdraw(Amount.of("99.99"), "overdraft")
        assertThat(account.findAll()).hasSize(2)

        val result = account.withdraw(Amount.of("0.01"), "overdraft")

        assertThat(result.isRight()).isTrue()
        assertThat(account.findAll()).hasSize(3)
    }

    @Test
    fun `transfer between two accounts`() {
        val (origin, originTransactionCount) = persistAndSize(AccountBuilder.aNew(this::account).clock(fakeClock).movements().build(), "origin")
        val (destination, destinationTransactionCount) = persistAndSize(AccountBuilder.aNew(this::account).clock(fakeClock).movements().build(), "destination")

        val result = Account.transfer(sampleTransferAmount, dummy_description, origin, destination)

        assertThat(origin.value.findAll().size).isEqualTo(originTransactionCount + 1)
        assertThat(destination.value.findAll().size).isEqualTo(destinationTransactionCount + 1)
        assertThat(result).isEqualTo(Received(
                Tx(sampleTransferAmount, FakeClock.date("14/03/2018"), dummy_description),
                Completed(origin.id, destination.id)
        ))
    }

    @Test
    fun `money is not lost during transfers`() {
        val (origin, _) = persistAndSize(AccountBuilder.aNew(this::account).clock(fakeClock).movements().build(), "origin")
        val (destination, _) = persistAndSize(AccountBuilder.aNew(this::account).clock(fakeClock).movements().build(), "destination")


        invariant({ Account.transfer(sampleTransferAmount, dummy_description, origin, destination) },
                { ("same balance" to origin.value.balance().add(destination.value.balance())) })
    }

    @Test
    fun `money is not lost during transfers with confirmation`() {
        val origin = Persisted.`for`(AccountBuilder.aNew(this::account).outgoing(securityProvider).clock(fakeClock).movements().build(), Id.of("origin"))
        val destination = Persisted.`for`(AccountBuilder.aNew(this::account).clock(fakeClock).movements().build(), Id.of("destination"))


        invariant({
            val x = Account.transfer(sampleTransferAmount, dummy_description, origin, destination) as Transaction.Transfer.Chain
            origin.value.confirmChain(x, securityProvider.generate())
        },
                { ("same balance" to origin.value.balance().add(destination.value.balance())) })
    }

    @Test
    fun `be protected with an OTP code to confirm a transfer`() {
        val account = AccountBuilder.aNew(this::account).clock(fakeClock).outgoing(securityProvider).movements().build()
        val origin = Persisted.`for`(account, Id.of("origin"))
        val initialBalance = origin.value.balance()
        val destination = Persisted.`for`(AccountBuilder.aNew(this::account).clock(fakeClock).build(), Id.of("destination"))


        val result = Account.transfer(sampleTransferAmount, dummy_description, origin, destination) as Transaction.Transfer.Chain

        verify(securityProvider).generate()
        assertThat(result.tx).isEqualTo(Tx(sampleTransferAmount, fakeClock.getTime(), dummy_description))
        assertThat(result.t1).isEqualTo(Intermediate(
                Tx(sampleTransferAmount, fakeClock.getTime(), dummy_description),
                Request.Request(origin, destination, securityProvider.generate())))
        assertThat(origin.value.balance()).isEqualTo(initialBalance)
    }


    @Test
    fun `transfer a security-enabled transfer after confirmation`() {
        val (origin, initialBalance) = withBalance(AccountBuilder.aNew(this::account).clock(fakeClock).outgoing(securityProvider).movements().build(), Id.of("origin"))
        val (destination, destinationBalance) = withBalance(AccountBuilder.aNew(this::account).clock(fakeClock).build(), Id.of("destination"))

        val result = Account.transfer(sampleTransferAmount, dummy_description, origin, destination) as Transaction.Transfer.Chain
        val result2 = result.let {
            origin.value.confirmChain(it, "123456")
        }

        val softly = SoftAssertions()
        softly.assertThat(result.tx).isEqualTo(Tx(sampleTransferAmount, fakeClock.getTime(), dummy_description))
        softly.assertThat(result.t1).isEqualTo(Intermediate(
                Tx(sampleTransferAmount, fakeClock.getTime(), dummy_description),
                Request.Request(origin, destination, "123456")
        ))
        softly.assertThat(origin.value.balance()).isEqualTo(initialBalance.subtract(sampleTransferAmount))
        softly.assertThat(destination.value.balance()).isEqualTo(destinationBalance.add(sampleTransferAmount))
        softly.assertAll()
        verify(securityProvider).generate()
    }

    private fun invariant(sideEffect: () -> Any, vararg functions: () -> Pair<String, Any>) {
        val before = functions.map { it.invoke() }

        sideEffect.invoke()

        val after = functions.map { it.invoke() }
        assertThat(after).isEqualTo(before)
    }

    private fun withBalance(account: Account, id: Id): Pair<Persisted<Account>, Amount> {
        return Pair(Persisted.`for`(account, id), account.balance())
    }

    protected fun persistAndSize(account: Account, id: String): Pair<Persisted<Account>, Int> {
        val persisted = Persisted.`for`(account, Id.of(id))
        val transactionCount = persisted.value.findAll().size
        return Pair(persisted, transactionCount)
    }

    private fun costsFor(account: Account) = account.findAll().map { it.value }.filter { it is Transaction.Cost }

    protected abstract fun account(): Account.AccountType

    private val fakeClock: Clock by lazy {
        FakeClock.reading(FakeClock.date("14/03/2018"))
    }

    protected val securityProvider = mock<Security> {
        on { generate() } doAnswer { println("Your code for verifying the operation is: ");"123456" }
    }

    val sampleTransferAmount = Amount.of("100")
    val dummy_description = "paying rent"

}

class AccountBuilder private constructor(private val accountType: () -> Account.AccountType) {
    companion object {
        fun aNew(param: () -> Account.AccountType): AccountBuilder {
            return AccountBuilder(param)
        }
    }

    private var outgoingSecurity: Option<Security> = None
    private var incomingSecurity: Option<Security> = None
    private var clock: Clock = Clock.aNew()
    private var movements: (Account) -> Unit = { _ -> Unit }


    fun outgoing(securityProvider: Security): AccountBuilder {
        this.outgoingSecurity = Some(securityProvider)
        return this
    }

    fun incoming(securityProvider: Security): AccountBuilder {
        this.incomingSecurity = Some(securityProvider)
        return this
    }

    fun clock(clock: Clock): AccountBuilder {
        this.clock = clock
        return this
    }

    fun movements(): AccountBuilder {
        this.movements = {
            it.deposit(Amount.of("100"), "first movement")
            it.deposit(Amount.of("200"), "second movement")
            it.withdraw(Amount.of("99"), "third movement")
        }
        return this
    }

    fun build(): Account {
        val finalService: IAccountService = decorateOutgoing(outgoingSecurity, decorateIncoming(incomingSecurity, AccountService()))
        val account = Account(clock, "account name", this.accountType.invoke(), finalService)
        this.movements(account)
        return account
    }

    private fun decorateOutgoing(security: Option<Security>, initial: IAccountService): IAccountService {
        return when (security) {
            is Some -> {
                OutgoingSecurityAccountService(initial, security.t)
            }
            is None -> {
                initial
            }
        }
    }

    private fun decorateIncoming(security: Option<Security>, initialValue: AccountService): IAccountService {
        return when (security) {
            is Some -> {
                IncomingSecurityAccountService(initialValue, security.t)
            }
            is None -> {
                initialValue
            }
        }
    }
}
