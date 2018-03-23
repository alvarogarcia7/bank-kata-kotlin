package com.example.kata.bank.service.delivery

import com.example.kata.bank.service.domain.AccountRequest
import com.example.kata.bank.service.domain.Id
import com.example.kata.bank.service.domain.Operation
import com.example.kata.bank.service.domain.Persisted
import com.example.kata.bank.service.domain.accounts.Account
import com.example.kata.bank.service.infrastructure.accounts.AccountRestrictedRepository
import com.example.kata.bank.service.infrastructure.operations.OperationsRepository
import com.example.kata.bank.service.infrastructure.statement.Statement

class StatementRequestInteractor(val accountRepository: AccountRestrictedRepository, val operationsRepository: OperationsRepository) {
    fun createAndSaveOperation(account: Account, create: AccountRequest): Id {
        val statement = create.apply<Statement>(account)
        val id = Id.random()
        operationsRepository.save(Persisted.`for`(Operation.Statement(statement), id))
        return id
    }
}