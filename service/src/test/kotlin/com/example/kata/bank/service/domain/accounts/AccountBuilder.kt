package com.example.kata.bank.service.domain.accounts

import arrow.core.None
import arrow.core.Option
import arrow.core.Some
import com.example.kata.bank.service.domain.transactions.Amount

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
        val account = Account(clock, "account name", this.accountType.invoke(), incomingSecurity, outgoingSecurity)
        this.movements(account)
        return account
    }
}