package com.example.kata.bank.service.infrastructure.storage

import arrow.core.Option
import com.example.kata.bank.service.domain.Id
import com.example.kata.bank.service.domain.Persisted
import com.example.kata.bank.service.domain.ReadRepository
import com.example.kata.bank.service.domain.SimpleWriteRepository

open class InMemorySimpleRepository<X>(private val values: MutableList<Persisted<X>>) : SimpleWriteRepository<X>, ReadRepository<X> by InMemoryReadRepository(values) {
    override fun save(entity: Persisted<X>) {
        this.values.add(entity)
    }
}

open class InMemoryReadRepository<X>(open val values: MutableList<Persisted<X>>) : ReadRepository<X> {
    override fun findAll(): List<Persisted<X>> {
        return values.toList()
    }

    override fun findBy(id: Id): Option<Persisted<X>> {
        return Option.fromNullable(values.find { it.id == id })
    }
}