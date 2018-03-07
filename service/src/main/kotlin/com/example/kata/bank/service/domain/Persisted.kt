package com.example.kata.bank.service.domain

data class Persisted<out T> private constructor(val value: T, val id: Id) {

    companion object {
        fun <T> `for`(value: T, id: Id): Persisted<T> {
            return Persisted(value, id)
        }
    }


}