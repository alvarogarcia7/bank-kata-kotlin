package com.example.kata.bank.service.domain

import arrow.core.Option
import java.util.*

class TransactionRepository {
    private val values = mutableListOf<Persisted<Transaction>>()
    fun save(transaction: Persisted<Transaction>) {
        values.add(transaction)
    }

    fun findAll(): List<Persisted<Transaction>> {
        return values
    }

    fun findBy(id: UUID): Option<Persisted<Transaction>> {
        return Option.fromNullable(values.first { it.id == id })
    }
}

