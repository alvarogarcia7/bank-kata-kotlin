package com.example.kata.bank.service.usecases.accounts

import arrow.core.Either
import com.example.kata.bank.service.domain.Persisted
import com.example.kata.bank.service.domain.accounts.Account
import com.example.kata.bank.service.domain.accounts.Clock
import com.example.kata.bank.service.infrastructure.accounts.AccountRestrictedRepository

class OpenAccountUseCase(private val accountRepository: AccountRestrictedRepository) {
    fun open(request: OpenAccountRequest): Either<List<Exception>, Persisted<Account>> {
        val account = Account(Clock.aNew(), request.name)
        return accountRepository.save(Persisted.random(account))
    }
}

data class OpenAccountRequest(val name: String)