package com.example.kata.bank.service.infrastructure.accounts

import arrow.core.Option
import com.example.kata.bank.service.domain.Account
import com.example.kata.bank.service.domain.AccountId
import com.example.kata.bank.service.domain.Persisted
import java.util.*

class AccountRepository {
    private val accounts = mutableListOf<Persisted<Account>>()

    fun findBy(accountId: AccountId): Option<Persisted<Account>> {
        return Option.fromNullable(accounts.find { it.id == UUID.fromString(accountId.value) })
    }

    fun save(entity: Persisted<Account>) {
        this.accounts.add(entity)
    }

    fun findAll(): List<Persisted<Account>> {
        return accounts.toList()
    }
}