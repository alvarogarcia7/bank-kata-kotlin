package com.example.kata.bank.service.infrastructure.transactions

import com.example.kata.bank.service.domain.transactions.Transaction
import com.example.kata.bank.service.infrastructure.storage.InMemorySimpleRepository

class TransactionSimpleRepository : InMemorySimpleRepository<Transaction>(mutableListOf())

