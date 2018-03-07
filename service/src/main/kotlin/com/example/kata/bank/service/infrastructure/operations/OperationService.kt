package com.example.kata.bank.service.infrastructure.operations

import arrow.core.Option
import com.example.kata.bank.service.domain.Id
import com.example.kata.bank.service.domain.accounts.Account
import com.example.kata.bank.service.domain.transactions.Amount

open class OperationService {
    fun deposit(account: Account?, depositRequest: OperationRequest.DepositRequest): Option<Id> {
        return Option.fromNullable(account?.deposit(toDomain(depositRequest.amount), depositRequest.description))
    }

    private fun toDomain(amount: AmountDTO): Amount {
        return Amount.of(amount.value)
    }
}