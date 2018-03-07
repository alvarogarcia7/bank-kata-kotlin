package com.example.kata.bank.service.delivery

import arrow.core.Either

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