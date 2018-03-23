package com.example.kata.bank.service.infrastructure

import com.example.kata.bank.service.domain.InMemorySimpleRepository
import com.example.kata.bank.service.domain.Operation

class OperationsSimpleRepository : InMemorySimpleRepository<Operation>()
