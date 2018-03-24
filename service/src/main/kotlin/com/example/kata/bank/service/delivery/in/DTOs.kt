package com.example.kata.bank.service.delivery.`in`

import arrow.core.Either
import com.example.kata.bank.service.domain.AccountRequest
import com.example.kata.bank.service.domain.transactions.Transaction

data class StatementDTO(val transactions: List<Transaction>)
data class StatementRequestDTO(val type: String) {
    fun validate(): Either<List<Exception>, AccountRequest.StatementRequest> {
        return Either.cond(this.type == "statement", { AccountRequest.StatementRequest.all() }, { listOf(Exception("This operation is not supported for now")) })
    }
}

data class OpenAccountRequestDTO(val name: String?) {
    fun validate(): Either<List<Exception>, OpenAccountRequestDTO> {
        var errors = mutableListOf<Exception>()
        if (name == null || name == "") {
            errors.add(IllegalArgumentException("empty/blank account name"))
        }
        return if (errors.isEmpty()) {
            Either.right(this)
        } else {
            Either.left(errors)
        }
    }
}