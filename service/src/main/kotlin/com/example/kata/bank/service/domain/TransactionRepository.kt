package com.example.kata.bank.service.domain

class TransactionRepository {
    private val values = mutableListOf<Transaction>()
    fun save(transaction: Transaction) {
        values.add(0, transaction)
    }

    fun findAll(): List<Transaction> {
        return values.toList()
    }

}