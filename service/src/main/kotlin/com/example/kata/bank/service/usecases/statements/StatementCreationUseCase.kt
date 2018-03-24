package com.example.kata.bank.service.usecases.statements

import com.example.kata.bank.service.domain.AccountRequest
import com.example.kata.bank.service.domain.Id
import com.example.kata.bank.service.domain.Operation
import com.example.kata.bank.service.domain.Persisted
import com.example.kata.bank.service.domain.accounts.Account
import com.example.kata.bank.service.infrastructure.operations.OperationsRepository
import com.example.kata.bank.service.infrastructure.statement.Statement

class StatementCreationUseCase(private val operationsRepository: OperationsRepository) {
    fun createStatement(account: Account, create: AccountRequest): Id {
        val statement = create.apply<Statement>(account)
        val id = Id.random()
        operationsRepository.save(Persisted.`for`(Operation.Statement(statement), id))
        return id
    }
}