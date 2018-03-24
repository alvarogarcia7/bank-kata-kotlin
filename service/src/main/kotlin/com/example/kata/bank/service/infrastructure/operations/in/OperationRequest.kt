package com.example.kata.bank.service.infrastructure.operations.`in`

import com.example.kata.bank.service.domain.accounts.Account
import com.example.kata.bank.service.domain.transactions.Amount
import com.example.kata.bank.service.infrastructure.operations.AmountDTO
import com.example.kata.bank.service.usecases.accounts.DepositUseCase
import com.example.kata.bank.service.usecases.accounts.TransferUseCase

sealed class OperationRequest {
    data class DepositRequest(val amount: AmountDTO, val description: String) : OperationRequest() {
        fun toUseCase(): DepositUseCase.Request {
            return DepositUseCase.Request(Amount.of(this.amount.value), this.description)
        }
    }

    data class TransferRequest(val amount: AmountDTO, val destination: AccountDTO, val description: String) : OperationRequest() {
        fun toUseCase(): TransferUseCase.In {
            val operationRequest = this
            val amount = Amount.of(operationRequest.amount.value)
            val description = operationRequest.description
            return TransferUseCase.In(Account.Number.of(operationRequest.destination.number), amount, description)
        }
    }
}

data class AccountDTO(val number: String, val owner: String)
