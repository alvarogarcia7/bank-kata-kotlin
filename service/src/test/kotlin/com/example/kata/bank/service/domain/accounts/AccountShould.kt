package com.example.kata.bank.service.domain.accounts

import com.example.kata.bank.service.domain.AccountRequest
import com.example.kata.bank.service.domain.FakeClock
import com.example.kata.bank.service.domain.Id
import com.example.kata.bank.service.domain.Persisted
import com.example.kata.bank.service.domain.transactions.Amount
import com.example.kata.bank.service.domain.transactions.Transaction
import com.nhaarman.mockitokotlin2.atLeast
import com.nhaarman.mockitokotlin2.doAnswer
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
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
            Account.transfer(sampleTransferAmount, dummy_description, origin, destination)
            confirmFirstPendingOutogoingTransfer(origin)
        },
                { ("same balance" to origin.value.balance().add(destination.value.balance())) })
    }

    @Test
    fun `be protected with an OTP code to confirm a transfer`() {
        val account = AccountBuilder.aNew(this::account).clock(fakeClock).outgoing(securityProvider).movements().build()
        val origin = Persisted.`for`(account, Id.of("origin"))
        val initialBalance = origin.value.balance()
        val destination = Persisted.`for`(AccountBuilder.aNew(this::account).clock(fakeClock).build(), Id.of("destination"))

        Account.transfer(sampleTransferAmount, dummy_description, origin, destination)

        verify(securityProvider).generate()
        assertThat(origin.value.balance()).isEqualTo(initialBalance)
    }

    @Test
    fun `transfer a security-enabled transfer after confirmation`() {
        val (origin, initialBalance) = withBalance(AccountBuilder.aNew(this::account).clock(fakeClock).outgoing(securityProvider).movements().build(), Id.of("origin"))
        val (destination, destinationBalance) = withBalance(AccountBuilder.aNew(this::account).clock(fakeClock).build(), Id.of("destination"))

        Account.transfer(sampleTransferAmount, dummy_description, origin, destination)
        confirmFirstPendingOutogoingTransfer(origin)

        val softly = SoftAssertions()
        softly.assertThat(origin.value.balance()).isEqualTo(initialBalance.subtract(sampleTransferAmount))
        softly.assertThat(destination.value.balance()).isEqualTo(destinationBalance.add(sampleTransferAmount))
        softly.assertAll()
        verify(securityProvider, atLeast(1)).generate()
    }

    fun confirmFirstPendingOutogoingTransfer(account: Persisted<Account>) {
        account.value.userConfirmOutgoing(account.value.pendingTransfers().entries.first().key, securityProvider.generate())
    }

    fun confirmFirstPendingIncomingTransfer(account: Persisted<Account>) {
        account.value.userConfirmIncoming(account.value.pendingTransfers().entries.first().key, securityProvider.generate())
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
        on { generate() } doAnswer { println("Your code for verifying the operation is: ");PinCode("123456") }
    }

    val sampleTransferAmount = Amount.of("100")
    val dummy_description = "paying rent"

}

