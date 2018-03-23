package com.example.kata.bank.service.domain.accounts

import arrow.core.Either
import arrow.core.Option
import com.example.kata.bank.service.domain.Id
import com.example.kata.bank.service.domain.Persisted
import com.example.kata.bank.service.domain.ReadRepository
import com.example.kata.bank.service.domain.RestrictedWriteRepository

class AccountRestrictedRepository : ReadRepository<Account>, RestrictedWriteRepository<Account> {
    protected val values = mutableListOf<Persisted<Account>>()
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

    override fun findAll(): List<Persisted<Account>> {
        return values.toList()
    }

    override fun findBy(id: Id): Option<Persisted<Account>> {
        return Option.fromNullable(values.find { it.id == id })
    }
}
