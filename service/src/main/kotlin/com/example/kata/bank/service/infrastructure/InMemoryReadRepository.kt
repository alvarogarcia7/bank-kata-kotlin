package com.example.kata.bank.service.infrastructure

import arrow.core.Option
import com.example.kata.bank.service.domain.Id
import com.example.kata.bank.service.domain.Persisted
import com.example.kata.bank.service.domain.ReadRepository

open class InMemoryReadRepository<X> : ReadRepository<X> {
    protected val values = mutableListOf<Persisted<X>>()
    override fun findAll(): List<Persisted<X>> {
        return values.toList()
    }

    override fun findBy(id: Id): Option<Persisted<X>> {
        return Option.fromNullable(values.find { it.id == id })
    }
}