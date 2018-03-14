package com.example.kata.bank.service.domain.accounts

import arrow.core.Either
import com.example.kata.bank.service.domain.AccountRequest
import com.example.kata.bank.service.domain.transactions.Amount
import org.assertj.core.api.Assertions
import org.junit.Test

internal class PersonalAccountShould : AccountShould() {
    @Test
    fun `create an unfiltered statement`() {
        val account = accountWithMovements()
        val statement = account.createStatement(AccountRequest.StatementRequest.all())

        Assertions.assertThat(statement.lines).hasSize(5) //1 (initial) + 3 (above) + 1 for the cost
    }

    @Test
    fun `cannot go overdraft`() {
        val account = account()
        Assertions.assertThat(account.findAll()).hasSize(0)

        val result = account.withdraw(Amount.Companion.of("1"), "overdraft")

        Assertions.assertThat(result.mapLeft { it.map { it.message } }).isEqualTo(Either.left(listOf("Cannot go overdraft")))
        Assertions.assertThat(account.findAll()).hasSize(0)
    }

    private fun accountWithMovements(): Account {
        val account = account()
        account.deposit(Amount.of("100"), "first movement")
        account.deposit(Amount.of("200"), "second movement")
        account.withdraw(Amount.of("99"), "third movement")
        return account
    }

    override fun account(): Account {
        return Account(Clock.aNew(), "test account", Account.AccountType.Personal)
    }
}
