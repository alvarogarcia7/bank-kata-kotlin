package com.example.kata.bank.service.domain.transactions

import com.example.kata.bank.service.infrastructure.storage.InMemorySimpleRepository

class TransactionSimpleRepository : InMemorySimpleRepository<Transaction>(mutableListOf())

