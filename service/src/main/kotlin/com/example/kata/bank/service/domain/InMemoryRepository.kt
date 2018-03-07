package com.example.kata.bank.service.domain

import arrow.core.Option

open class InMemoryRepository<X> {
    private val values = mutableListOf<Persisted<X>>()

    fun save(entity: Persisted<X>) {
        this.values.add(entity)
    }

    fun findAll(): List<Persisted<X>> {
        return values.toList()
    }

    fun findBy(id: Id): Option<Persisted<X>> {
        return Option.fromNullable(values.find { it.id == id })
    }
}