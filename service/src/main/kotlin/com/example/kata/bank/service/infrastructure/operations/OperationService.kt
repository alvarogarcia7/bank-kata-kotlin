package com.example.kata.bank.service.infrastructure.operations

import arrow.core.Option
import com.example.kata.bank.service.domain.accounts.Account
import com.example.kata.bank.service.domain.transactions.Amount
import java.util.*

open class OperationService {
    fun deposit(account: Account?, depositRequest: OperationRequest.DepositRequest): Option<UUID> {
        return Option.fromNullable(account?.deposit(toDomain(depositRequest.amount), depositRequest.description))
    }

    private fun toDomain(amount: AmountDTO): Amount {
        return Amount.of(amount.value)
    }
}