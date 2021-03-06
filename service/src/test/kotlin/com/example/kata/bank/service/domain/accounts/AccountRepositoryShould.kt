package com.example.kata.bank.service.domain.accounts

import arrow.core.Either
import com.example.kata.bank.service.domain.Id
import com.example.kata.bank.service.domain.Persisted
import com.example.kata.bank.service.infrastructure.accounts.AccountRestrictedRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class AccountRepositoryShould {
    @Test
    fun `make the account number primary key`() {
        val repository = AccountRestrictedRepository.aNew()
        repository.save(Persisted.`for`(Account(clock = Clock.aNew(), name = "name1", number = Account.Number.of("11")), Id.random()))

        val result = repository.save(Persisted.`for`(Account(clock = Clock.aNew(), name = "name1", number = Account.Number.of("11")), Id.random()))

        assertThat(result.mapLeft { it.map { it.message } }).isEqualTo(Either.left(listOf("Already exists account number: 11")))
    }
}