package com.example.kata.bank.service.infrastructure

import arrow.core.Either
import arrow.core.Option
import com.example.kata.bank.service.domain.Persisted
import com.example.kata.bank.service.domain.ReadRepository
import com.example.kata.bank.service.domain.RestrictedWriteRepository
import com.example.kata.bank.service.domain.accounts.Account
import com.example.kata.bank.service.infrastructure.storage.InMemoryReadRepository

class AccountRestrictedRepository(private val values: MutableList<Persisted<Account>>) :
        ReadRepository<Account> by InMemoryReadRepository(values),
        RestrictedWriteRepository<Account> {
    override fun save(entity: Persisted<Account>): Either<List<Exception>, Persisted<Account>> {
        if (findBy(entity.value.number).isDefined()) {
            return Either.left(listOf(IllegalArgumentException("Already exists account number: ${entity.value.number.value}")))
        } else {
            values.add(entity)
            return Either.right(entity)
        }
    }

    fun findBy(accountNumber: Account.Number): Option<Persisted<Account>> {
        return Option.fromNullable(this.values.find { it.value.number == accountNumber })
    }

    companion object {
        fun aNew(): AccountRestrictedRepository {
            return AccountRestrictedRepository(mutableListOf())
        }
    }
}
