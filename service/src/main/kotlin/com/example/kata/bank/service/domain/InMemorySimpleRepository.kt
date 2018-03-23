package com.example.kata.bank.service.domain

import arrow.core.Either
import arrow.core.Option

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