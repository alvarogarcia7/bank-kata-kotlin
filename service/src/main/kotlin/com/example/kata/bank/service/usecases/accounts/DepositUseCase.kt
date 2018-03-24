package com.example.kata.bank.service.usecases.accounts

import arrow.core.Either
import com.example.kata.bank.service.common.toEither
import com.example.kata.bank.service.domain.Id
import com.example.kata.bank.service.domain.transactions.Amount
import com.example.kata.bank.service.infrastructure.accounts.AccountRestrictedRepository

open class DepositUseCase(private val accountRepository: AccountRestrictedRepository) {
    open fun deposit(accountId: Id, request: Request): Either<List<Exception>, Id> {
        val result = accountRepository.findBy(accountId)
                .toEither({ listOf(Exception("No account")) })
                .map { it.value.deposit(request.amount, request.description) }
        return result
    }

    data class Request(val amount: Amount, val description: String)

}
