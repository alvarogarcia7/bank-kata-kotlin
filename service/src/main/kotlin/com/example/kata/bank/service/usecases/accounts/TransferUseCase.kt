package com.example.kata.bank.service.usecases.accounts

import arrow.core.Either
import com.example.kata.bank.service.domain.Id
import com.example.kata.bank.service.domain.accounts.Account
import com.example.kata.bank.service.domain.transactions.Amount
import com.example.kata.bank.service.infrastructure.accounts.AccountRestrictedRepository

class TransferUseCase(private val accountRepository: AccountRestrictedRepository) {
    fun transfer(accountId: Id, operationRequest: In): Either<Nothing, Id> {
        accountRepository.findBy(operationRequest.destination)
                .flatMap { to ->
                    accountRepository.findBy(accountId)
                            .map { from ->
                                Account.transfer(operationRequest.amount, operationRequest.description, from, to)
                            }
                }
        return Either.right(Id.random())
    }

    data class In(val destination: Account.Number, val amount: Amount, val description: String)
}