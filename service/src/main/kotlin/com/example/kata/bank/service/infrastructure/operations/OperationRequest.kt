package com.example.kata.bank.service.infrastructure.operations

sealed class OperationRequest {
    data class DepositRequest(val amount: AmountDTO, val description: String) : OperationRequest()
}