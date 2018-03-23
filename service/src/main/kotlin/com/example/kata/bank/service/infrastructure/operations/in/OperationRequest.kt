package com.example.kata.bank.service.infrastructure.operations.`in`

import com.example.kata.bank.service.infrastructure.operations.AmountDTO

sealed class OperationRequest {
    data class DepositRequest(val amount: AmountDTO, val description: String) : OperationRequest()
    data class TransferRequest(val amount: AmountDTO, val destination: AccountDTO, val description: String) : OperationRequest()
}

data class AccountDTO(val number: String, val owner: String)
