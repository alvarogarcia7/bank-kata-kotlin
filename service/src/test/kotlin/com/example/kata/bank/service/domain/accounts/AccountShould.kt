package com.example.kata.bank.service.domain.accounts

import arrow.core.Either
import com.example.kata.bank.service.domain.AccountRequest
import com.example.kata.bank.service.domain.FakeClock
import com.example.kata.bank.service.domain.Id
import com.example.kata.bank.service.domain.Persisted
import com.example.kata.bank.service.domain.transactions.Amount
import com.example.kata.bank.service.domain.transactions.Transaction
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

abstract class AccountShould {
    @Test
    fun `create a filtered statement, just the Deposits`() {
        val account = accountWithMovements(Clock.aNew())

        val statement = account.createStatement(AccountRequest.StatementRequest.filter { it is Transaction.Deposit })

        Assertions.assertThat(statement.lines).hasSize(3) //1 (initial) + 2 (above)
    }

    @Test
    fun `create a filtered statement, just the Withdrawals`() {
        val account = accountWithMovements(Clock.aNew())

        val statement = account.createStatement(AccountRequest.StatementRequest.filter { it is Transaction.Withdrawal })

        Assertions.assertThat(statement.lines).hasSize(2) //1 (initial) + 1 (above)
    }

    @Test
    fun `create a filtered statement --that produces no results-- does not cost the personal user a dime`() {
        val account = accountWithMovements(Clock.aNew())

        account.createStatement(AccountRequest.StatementRequest.filter { its -> false })

        Assertions.assertThat(costsFor(account)).hasSize(0)
    }

    @Test
    fun `can withdraw even if it would empty the account `() {
        val account = account(Clock.aNew())
        account.deposit(Amount.of("100"),
                "initial deposit")
        Assertions.assertThat(account.findAll()).hasSize(1)

        val result = account.withdraw(Amount.Companion.of("100"), "overdraft")

        Assertions.assertThat(result.isRight()).isTrue()
        Assertions.assertThat(account.findAll()).hasSize(2)
    }

    @Test
    fun `can perform multiple withdraws even if it would empty the account`() {
        val account = account(Clock.aNew())
        account.deposit(Amount.of("100"), "initial deposit")
        account.withdraw(Amount.Companion.of("99.99"), "overdraft")
        Assertions.assertThat(account.findAll()).hasSize(2)

        val result = account.withdraw(Amount.Companion.of("0.01"), "overdraft")

        Assertions.assertThat(result.isRight()).isTrue()
        Assertions.assertThat(account.findAll()).hasSize(3)
    }


    @Test
    fun `transfer between two accounts`() {
        val date1 = FakeClock.date("14/03/2018")
        val clock = FakeClock.reading(date1)
        val origin = accountWithMovements(clock)
        val originTransactionCount = origin.findAll().size
        val destination = Persisted.`for`(account(clock), Id.of("destination"))
        val destinationTransactionCount = destination.value.findAll().size

        val operationAmount = Amount.of("100")
        val description = "paying rent"

        val result = origin.transfer(operationAmount, description, destination)

        assertThat(origin.findAll().size).isEqualTo(originTransactionCount + 1)
        assertThat(destination.value.findAll().size).isEqualTo(destinationTransactionCount + 1)
        Assertions.assertThat(result.isRight()).isTrue()
        assertThat(result).isEqualTo(Either.right(Transaction.Transfer(operationAmount, date1, description, destination.id)))
    }


    private fun costsFor(account: Account) = account.findAll().map { it.value }.filter { it is Transaction.Cost }

    private fun accountWithMovements(clock: Clock): Account {
        val account = account(clock)
        account.deposit(Amount.of("100"), "first movement")
        account.deposit(Amount.of("200"), "second movement")
        account.withdraw(Amount.of("99"), "third movement")
        return account
    }

    protected abstract fun account(clock: Clock): Account
}