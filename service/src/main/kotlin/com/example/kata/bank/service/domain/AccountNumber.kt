package com.example.kata.bank.service.domain

data class AccountNumber private constructor(val value: String) {
    companion object {
        fun of(value: String): AccountNumber {
            return AccountNumber(value)
        }
    }
}
