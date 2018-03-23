package com.example.kata.bank.service.infrastructure.operations

import com.example.kata.bank.service.domain.Operation
import com.example.kata.bank.service.infrastructure.storage.InMemorySimpleRepository

class OperationsRepository : InMemorySimpleRepository<Operation>(mutableListOf())
