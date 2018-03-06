package com.example.kata.bank.service.infrastructure.operations

import com.example.kata.bank.service.domain.Account
import com.example.kata.bank.service.domain.Amount

open class OperationService {
    fun deposit(account: Account?, depositRequest: OperationRequest.DepositRequest) {
        account?.deposit(toDomain(depositRequest.amount), depositRequest.description)
    }

    private fun toDomain(amount: AmountDTO): Amount {
        return Amount.of(amount.value)
    }
}