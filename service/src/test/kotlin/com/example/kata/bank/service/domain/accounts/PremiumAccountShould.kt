package com.example.kata.bank.service.domain.accounts

import com.example.kata.bank.service.domain.AccountRequest
import com.example.kata.bank.service.domain.transactions.Amount
import com.example.kata.bank.service.domain.transactions.Transaction
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class PremiumAccountShould : AccountShould() {
    @Test
    fun `create an unfiltered statement`() {
        val account = accountWithMovements()
        val statement = account.createStatement(AccountRequest.StatementRequest.all())

        assertThat(statement.lines).hasSize(4) //1 (initial) + 3 (above) // no cost
    }

    @Test
    fun `calculate the balance for an account without movements`() {
        val account = AccountBuilder.aNew(this::account).build()
        assertThat(account.balance()).isEqualTo(Amount.of("0"))
    }

    @Test
    fun `calculate the balance for an account with some movements`() {
        val account = accountWithMovements()

        assertThat(account.balance()).isEqualTo(Amount.of("201"))
    }

    @Test
    fun `create a filtered statement --with results-- does not cost the premium user a dime`() {
        val account = accountWithMovements()

        account.createStatement(AccountRequest.StatementRequest.all())

        assertThat(costsFor(account)).hasSize(0)
    }

    @Test
    fun `create a filtered statement --without results-- does not cost the premium user a dime`() {
        val account = accountWithMovements()

        account.createStatement(AccountRequest.StatementRequest.filter { it -> false })

        assertThat(account.findAll().map { it.value }.filter { it is Transaction.Cost }).hasSize(0)
    }

    @Test
    fun `can go overdraft`() {
        val account = AccountBuilder.aNew(this::account).build()

        val result = account.withdraw(Amount.of("100"), "another expense")

        assertThat(result.isRight()).isTrue()
    }

    @Test
    fun `can confirm incoming transfers`() {
        val (sender, _) = persistAndSize(AccountBuilder.aNew(this::account).movements().build(), "sender")
        val (receiver, _) = persistAndSize(AccountBuilder.aNew(this::account).receivingSecurity(securityProvider).build(), "receiver")
        val previousBalance = receiver.value.balance()

        val result = Account.transfer(Amount.of("100"), "scam transfer", sender, receiver)
        //do not confirm incoming transfer

        assertThat(receiver.value.balance()).isEqualTo(previousBalance)
    }

    @Test
    fun `can confirm incoming secure transfers`() {
        val (sender, _) = persistAndSize(AccountBuilder.aNew(this::account).security(securityProvider).movements().build(), "sender")
        val (receiver, _) = persistAndSize(AccountBuilder.aNew(this::account).receivingSecurity(securityProvider).build(), "receiver")
        val previousBalance = receiver.value.balance()

        val result = Account.transfer(Amount.of("100"), "scam transfer", sender, receiver)
        //do not confirm incoming transfer

        assertThat(receiver.value.balance()).isEqualTo(previousBalance)
    }


    private fun costsFor(account: Account) = account.findAll().map { it.value }.filter { it is Transaction.Cost }

    private fun accountWithMovements(): Account {
        return AccountBuilder.aNew(this::account).movements().build()
    }

    override fun account(): Account.AccountType {
        return Account.AccountType.Premium
    }
}