package com.example.kata.bank.service.domain

import java.util.*

data class Persisted<out T> private constructor(val value: T, val id: UUID) {

    companion object {
        fun <T> `for`(value: T, id: UUID): Persisted<T> {
            return Persisted(value, id)
        }
    }


}