package com.example.kata.bank.service.infrastructure

import arrow.core.Option
import com.example.kata.bank.service.domain.Id
import com.example.kata.bank.service.domain.Persisted
import com.example.kata.bank.service.domain.transactions.Transaction

//TODO AGB - Still thinking about this pattern
class AccountsService(private val repository: AccountRestrictedRepository) {
    fun operationsFor(accountId: Id): Option<List<Persisted<Transaction>>> {
        return repository
                .findBy(accountId)
                .map { account -> account.value.findAll() }
    }
}
