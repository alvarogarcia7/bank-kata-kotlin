package com.example.kata.bank.service.domain.accounts

import arrow.core.Either

data class OpenAccountRequest private constructor(val name: String) {
    companion object {
        fun parse(name: String): Either<List<Exception>, Account> {
            return Either.right(Account(Clock.aNew(), name))
        }
    }
}