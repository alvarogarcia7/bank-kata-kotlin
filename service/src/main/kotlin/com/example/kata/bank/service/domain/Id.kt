package com.example.kata.bank.service.domain

import java.util.*

data class Id private constructor(val value: String) {
    companion object {
        fun random(): Id {
            return Id.of(UUID.randomUUID().toString())
        }

        fun of(value: String): Id {
            return Id(value)
        }
    }
}