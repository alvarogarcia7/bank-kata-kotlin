package com.example.kata.bank.service.domain.accounts

import arrow.core.Option
import com.example.kata.bank.service.domain.InMemoryRepository
import com.example.kata.bank.service.domain.Persisted
import java.util.*

class AccountRepository : InMemoryRepository<Account>() {
    private val accounts = mutableListOf<Persisted<Account>>()

    fun findBy(accountId: AccountId): Option<Persisted<Account>> {
        return Option.fromNullable(accounts.find { it.id == UUID.fromString(accountId.value) })
    }
}