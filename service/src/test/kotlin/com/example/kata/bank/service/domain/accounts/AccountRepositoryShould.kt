package com.example.kata.bank.service.domain.accounts

import arrow.core.Either
import com.example.kata.bank.service.domain.AccountNumber
import com.example.kata.bank.service.domain.Id
import com.example.kata.bank.service.domain.Persisted
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class AccountRepositoryShould {
    @Test
    fun `make_the_account_number_primary_key`() {
        val repository = AccountRestrictedRepository()
        repository.save(Persisted.`for`(Account(Clock.aNew(), "name1", number = AccountNumber.of("11")), Id.random()))

        val result = repository.save(Persisted.`for`(Account(Clock.aNew(), "name1", number = AccountNumber.of("11")), Id.random()))

        assertThat(result.mapLeft { it.map { it.message } }).isEqualTo(Either.left(listOf("Already exists account number: 11")))
    }
}