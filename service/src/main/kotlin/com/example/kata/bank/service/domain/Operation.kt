package com.example.kata.bank.service.domain

sealed class Operation {
    data class Statement(val statement: com.example.kata.bank.service.infrastructure.statement.Statement) : Operation()
}
