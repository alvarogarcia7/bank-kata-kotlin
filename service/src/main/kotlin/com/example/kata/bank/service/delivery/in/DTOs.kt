package com.example.kata.bank.service.delivery.`in`

import arrow.core.Either
import com.example.kata.bank.service.domain.transactions.Transaction

data class StatementDTO(val transactions: List<Transaction>)
data class StatementRequestDTO(val type: String) {
    fun validate(): Either<List<Exception>, StatementRequestDTO> {
        return Either.cond(this.type == "statement", { this }, { listOf(Exception("This operation is not supported for now")) })
    }
}