package com.example.kata.bank.service.domain.accounts

import arrow.core.Option
import com.example.kata.bank.service.domain.AccountNumber
import com.example.kata.bank.service.domain.InMemoryRepository
import com.example.kata.bank.service.domain.Persisted

class AccountRepository : InMemoryRepository<Account>() {
    fun findBy(accountNumber: AccountNumber): Option<Persisted<Account>> {
        return Option.fromNullable(this.values.find { it.value.number == accountNumber })
    }
}