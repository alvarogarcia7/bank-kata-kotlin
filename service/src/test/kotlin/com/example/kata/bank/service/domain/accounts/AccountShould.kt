package com.example.kata.bank.service.domain.accounts

import com.example.kata.bank.service.domain.AccountRequest
import com.example.kata.bank.service.domain.transactions.Amount
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class AccountShould {
    @Test
    fun `calculate the balance for an account without movements`() {
        val account = Account(Clock.aNew(), "test account")
        Assertions.assertThat(account.balance()).isEqualTo(Amount.of("0"))
    }

    @Test
    fun `calculate the balance for an account with some movements`() {
        val account = Account(Clock.aNew(), "test account")
        account.deposit(Amount.Companion.of("100"), "first movement")
        account.deposit(Amount.Companion.of("200"), "second movement")
        account.withdraw(Amount.Companion.of("99"), "third movement")

        Assertions.assertThat(account.balance()).isEqualTo(Amount.of("201"))
    }

    @Test
    fun `create an unfiltered statement`() {
        val account = Account(Clock.aNew(), "test account")
        account.deposit(Amount.Companion.of("100"), "first movement")
        account.deposit(Amount.Companion.of("200"), "second movement")
        account.withdraw(Amount.Companion.of("99"), "third movement")
        val statement = account.createStatement(AccountRequest.StatementRequest.all())

        assertThat(statement.lines).hasSize(5) //1 (initial) + 3 (above) + 1 for the cost
    }
}