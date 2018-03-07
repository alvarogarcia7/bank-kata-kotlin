package com.example.kata.bank.service.infrastructure.accounts

import com.example.kata.bank.service.domain.Persisted

open class InMemoryRepository<X> {
    private val values = mutableListOf<Persisted<X>>()

    fun save(entity: Persisted<X>) {
        this.values.add(entity)
    }

    fun findAll(): List<Persisted<X>> {
        return values.toList()
    }
}