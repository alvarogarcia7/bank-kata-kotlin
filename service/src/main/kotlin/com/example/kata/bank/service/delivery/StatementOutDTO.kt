package com.example.kata.bank.service.delivery

import com.example.kata.bank.service.infrastructure.operations.TransactionDTO

data class StatementOutDTO(val operations: List<TransactionDTO>)
