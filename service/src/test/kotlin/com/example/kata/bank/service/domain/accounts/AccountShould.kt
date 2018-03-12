package com.example.kata.bank.service.domain.accounts

import com.example.kata.bank.service.domain.AccountRequest
import com.example.kata.bank.service.domain.transactions.Amount
import com.example.kata.bank.service.domain.transactions.Transaction
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
        val account = accountWithMovements(Account.AccountType.Personal)

        Assertions.assertThat(account.balance()).isEqualTo(Amount.of("201"))
    }

    @Test
    fun `create an unfiltered statement`() {
        val account = accountWithMovements(Account.AccountType.Personal)
        val statement = account.createStatement(AccountRequest.StatementRequest.all())

        assertThat(statement.lines).hasSize(5) //1 (initial) + 3 (above) + 1 for the cost
    }


    @Test
    fun `create a filtered statement, just the Deposits`() {
        val account = accountWithMovements(Account.AccountType.Personal)

        val statement = account.createStatement(AccountRequest.StatementRequest.filter { it is Transaction.Deposit })

        assertThat(statement.lines).hasSize(3) //1 (initial) + 2 (above)
    }

    @Test
    fun `create a filtered statement, just the Withdrawals`() {
        val account = accountWithMovements(Account.AccountType.Personal)

        val statement = account.createStatement(AccountRequest.StatementRequest.filter { it is Transaction.Withdrawal })

        assertThat(statement.lines).hasSize(2) //1 (initial) + 1 (above)
    }

    @Test
    fun `create a filtered statement --that produces no results-- does not cost the personal user a dime`() {
        val account = accountWithMovements(Account.AccountType.Personal)

        account.createStatement(AccountRequest.StatementRequest.filter { it -> false })

        assertThat(account.findAll().map { it.value }.filter { it is Transaction.Cost }).hasSize(0)
    }

    @Test
    fun `create a filtered statement --with results-- does not cost the premium user a dime`() {
        val account = accountWithMovements(Account.AccountType.Premium)

        account.createStatement(AccountRequest.StatementRequest.all())

        assertThat(account.findAll().map { it.value }.filter { it is Transaction.Cost }).hasSize(0)
    }

    private fun accountWithMovements(type: Account.AccountType): Account {
        val account = Account(Clock.aNew(), "test account", type)
        account.deposit(Amount.of("100"), "first movement")
        account.deposit(Amount.of("200"), "second movement")
        account.withdraw(Amount.of("99"), "third movement")
        return account
    }
}