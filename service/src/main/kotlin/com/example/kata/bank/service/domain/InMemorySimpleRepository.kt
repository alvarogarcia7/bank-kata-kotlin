package com.example.kata.bank.service.domain

import arrow.core.Either
import arrow.core.Option

open class InMemorySimpleRepository<X> : ReadRepository<X>, SimpleWriteRepository<X> {
    protected val values = mutableListOf<Persisted<X>>()
    override fun save(entity: Persisted<X>) {
        this.values.add(entity)
    }

    override fun findAll(): List<Persisted<X>> {
        return values.toList()
    }

    override fun findBy(id: Id): Option<Persisted<X>> {
        return Option.fromNullable(values.find { it.id == id })
    }
}

interface SimpleWriteRepository<X> {
    fun save(entity: Persisted<X>)
}

interface ReadRepository<X> {
    fun findAll(): List<Persisted<X>>

    fun findBy(id: Id): Option<Persisted<X>>
}

interface RestrictedWriteRepository<X> {
    fun save(entity: Persisted<X>): Either<List<Exception>, Persisted<X>>
}