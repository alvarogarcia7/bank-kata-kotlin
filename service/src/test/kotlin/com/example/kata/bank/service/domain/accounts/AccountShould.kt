package com.example.kata.bank.service.domain.accounts

import arrow.core.Either
import com.example.kata.bank.service.domain.AccountRequest
import com.example.kata.bank.service.domain.FakeClock
import com.example.kata.bank.service.domain.Id
import com.example.kata.bank.service.domain.Persisted
import com.example.kata.bank.service.domain.transactions.Amount
import com.example.kata.bank.service.domain.transactions.Transaction
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

abstract class AccountShould {
    @Test
    fun `create a filtered statement, just the Deposits`() {
        val account = accountWithMovements()

        val statement = account.createStatement(AccountRequest.StatementRequest.filter { it is Transaction.Deposit })

        assertThat(statement.lines).hasSize(3) //1 (initial) + 2 (above)
    }

    @Test
    fun `create a filtered statement, just the Withdrawals`() {
        val account = accountWithMovements()

        val statement = account.createStatement(AccountRequest.StatementRequest.filter { it is Transaction.Withdrawal })

        assertThat(statement.lines).hasSize(2) //1 (initial) + 1 (above)
    }

    @Test
    fun `create a filtered statement --that produces no results-- does not cost the personal user a dime`() {
        val account = accountWithMovements()

        account.createStatement(AccountRequest.StatementRequest.filter { its -> false })

        assertThat(costsFor(account)).hasSize(0)
    }

    @Test
    fun `can withdraw even if it would empty the account `() {
        val account = account()
        account.deposit(Amount.of("100"),
                "initial deposit")
        assertThat(account.findAll()).hasSize(1)

        val result = account.withdraw(Amount.Companion.of("100"), "overdraft")

        assertThat(result.isRight()).isTrue()
        assertThat(account.findAll()).hasSize(2)
    }

    @Test
    fun `can perform multiple withdraws even if it would empty the account`() {
        val account = account()
        account.deposit(Amount.of("100"), "initial deposit")
        account.withdraw(Amount.Companion.of("99.99"), "overdraft")
        assertThat(account.findAll()).hasSize(2)

        val result = account.withdraw(Amount.Companion.of("0.01"), "overdraft")

        assertThat(result.isRight()).isTrue()
        assertThat(account.findAll()).hasSize(3)
    }

    @Test
    fun `transfer between two accounts`() {
        val date1 = FakeClock.date("14/03/2018")
        val clock = FakeClock.reading(date1)
        val (origin, originTransactionCount) = account_(clock, "origin")
        val (destination, destinationTransactionCount) = account_(clock, "destination")

        val operationAmount = Amount.of("100")
        val description = "paying rent"

        val result = Account.transfer(operationAmount, description, origin, destination)

        assertThat(origin.value.findAll().size).isEqualTo(originTransactionCount + 1)
        assertThat(destination.value.findAll().size).isEqualTo(destinationTransactionCount + 1)
        assertThat(result).isEqualTo(Either.right(Transaction.Transfer(operationAmount, date1, description, origin.id, destination.id)))
    }

    @Test
    fun `money is not lost during transfers`() {
        val clock = FakeClock.reading(FakeClock.date("14/03/2018"))
        val (origin, _) = account_(clock, "origin")
        val (destination, _) = account_(clock, "destination")


        val operationAmount = Amount.of("100")
        val description = "paying rent"

        invariant({ Account.transfer(operationAmount, description, origin, destination) },
                { ("same balance" to origin.value.balance().add(destination.value.balance())) },
                { ("same balance2" to origin.value.balance().add(destination.value.balance())) })
    }

    private fun invariant(sideEffect: () -> Any, vararg functions: () -> Pair<String, Any>) {
        val before = functions.map { it.invoke() }

        sideEffect.invoke()

        val after = functions.map { it.invoke() }
        assertThat(after).isEqualTo(before)
    }

    private fun account_(clock: Clock, accountId: String): Pair<Persisted<Account>, Int> {
        val account = Persisted.`for`(accountWithMovements(clock), Id.of(accountId))
        val transactionCount = account.value.findAll().size
        return Pair(account, transactionCount)
    }

    private fun costsFor(account: Account) = account.findAll().map { it.value }.filter { it is Transaction.Cost }

    private fun accountWithMovements() = accountWithMovements(Clock.aNew())

    private fun accountWithMovements(clock: Clock): Account {
        val account = account(clock)
        account.deposit(Amount.of("100"), "first movement")
        account.deposit(Amount.of("200"), "second movement")
        account.withdraw(Amount.of("99"), "third movement")
        return account
    }

    private fun account() = account(Clock.aNew())

    protected abstract fun account(clock: Clock): Account
}