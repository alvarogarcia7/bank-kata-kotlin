package com.example.kata.bank.service.domain.accounts

import arrow.core.Either
import com.example.kata.bank.service.domain.AccountRequest
import com.example.kata.bank.service.domain.transactions.Amount
import com.example.kata.bank.service.domain.transactions.Transaction
import org.assertj.core.api.Assertions
import org.junit.Test

abstract class AccountShould {
    @Test
    fun `create a filtered statement, just the Deposits`() {
        val account = accountWithMovements()

        val statement = account.createStatement(AccountRequest.StatementRequest.filter { it is Transaction.Deposit })

        Assertions.assertThat(statement.lines).hasSize(3) //1 (initial) + 2 (above)
    }

    @Test
    fun `create a filtered statement, just the Withdrawals`() {
        val account = accountWithMovements()

        val statement = account.createStatement(AccountRequest.StatementRequest.filter { it is Transaction.Withdrawal })

        Assertions.assertThat(statement.lines).hasSize(2) //1 (initial) + 1 (above)
    }

    @Test
    fun `create a filtered statement --that produces no results-- does not cost the personal user a dime`() {
        val account = accountWithMovements()

        account.createStatement(AccountRequest.StatementRequest.filter { its -> false })

        Assertions.assertThat(costsFor(account)).hasSize(0)
    }

    @Test
    fun `can withdraw even if it would empty the account `() {
        val account = account()
        account.deposit(Amount.of("100"),
                "initial deposit")
        Assertions.assertThat(account.findAll()).hasSize(1)

        val result = account.withdraw(Amount.Companion.of("100"), "overdraft")

        Assertions.assertThat(result.isRight()).isTrue()
        Assertions.assertThat(account.findAll()).hasSize(2)
    }

    @Test
    fun `cannot withdraw if it would go into overdraft`() {
        val account = account()
        Assertions.assertThat(account.findAll()).hasSize(0)

        val result = account.withdraw(Amount.Companion.of("1"), "overdraft")

        Assertions.assertThat(result.mapLeft { it.map { it.message } }).isEqualTo(Either.left(listOf("Cannot go overdraft")))
        Assertions.assertThat(account.findAll()).hasSize(0)
    }

    @Test
    fun `can perform multiple withdraws even if it would empty the account`() {
        val account = account()
        account.deposit(Amount.of("100"), "initial deposit")
        account.withdraw(Amount.Companion.of("99.99"), "overdraft")
        Assertions.assertThat(account.findAll()).hasSize(2)

        val result = account.withdraw(Amount.Companion.of("0.01"), "overdraft")

        Assertions.assertThat(result.isRight()).isTrue()
        Assertions.assertThat(account.findAll()).hasSize(3)
    }

    private fun costsFor(account: Account) = account.findAll().map { it.value }.filter { it is Transaction.Cost }

    private fun accountWithMovements(): Account {
        val account = account()
        account.deposit(Amount.of("100"), "first movement")
        account.deposit(Amount.of("200"), "second movement")
        account.withdraw(Amount.of("99"), "third movement")
        return account
    }

    protected abstract fun account(): Account
}