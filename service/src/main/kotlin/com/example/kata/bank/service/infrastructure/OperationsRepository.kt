package com.example.kata.bank.service.infrastructure

import com.example.kata.bank.service.domain.InMemoryRepository
import com.example.kata.bank.service.domain.Operation

class OperationsRepository : InMemoryRepository<Operation>()
