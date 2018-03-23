package com.example.kata.bank.service.domain.accounts.personal

import arrow.core.Either
import com.example.kata.bank.service.domain.AccountRequest
import com.example.kata.bank.service.domain.accounts.Account
import com.example.kata.bank.service.domain.accounts.AccountBuilder
import com.example.kata.bank.service.domain.accounts.AccountShould
import com.example.kata.bank.service.domain.transactions.Amount
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

internal class PersonalAccountShould : AccountShould() {
    @Test
    fun `create an unfiltered statement`() {
        val account = AccountBuilder.aNew(this::account).movements().build()
        val statement = account.createStatement(AccountRequest.StatementRequest.all())

        assertThat(statement.lines).hasSize(5) //1 (initial) + 3 (above) + 1 for the cost
    }

    @Test
    fun `cannot go overdraft`() {
        val account = AccountBuilder.aNew(this::account).build()
        assertThat(account.findAll()).hasSize(0)

        val result = account.withdraw(Amount.Companion.of("1"), "overdraft")

        assertThat(result.mapLeft { it.map { it.message } }).isEqualTo(Either.left(listOf("Cannot go overdraft")))
        assertThat(account.findAll()).hasSize(0)
    }

    override fun account(): Account.AccountType {
        return Account.AccountType.Personal
    }
}
