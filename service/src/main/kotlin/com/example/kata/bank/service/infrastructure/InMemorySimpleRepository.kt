package com.example.kata.bank.service.infrastructure

import com.example.kata.bank.service.domain.Persisted
import com.example.kata.bank.service.domain.SimpleWriteRepository

open class InMemorySimpleRepository<X> : SimpleWriteRepository<X>, InMemoryReadRepository<X>() {
    override fun save(entity: Persisted<X>) {
        this.values.add(entity)
    }
}