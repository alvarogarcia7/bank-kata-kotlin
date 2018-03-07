package com.example.kata.bank.service.infrastructure.operations

data class TransactionDTO(val amount: AmountDTO, val description: String, val time: TimeDTO)