package com.example.kata.bank.service.infrastructure.operations

import java.util.*

data class AmountDTO private constructor(val value: String) {
    val currency = Currency.getInstance("EUR")

    companion object {
        fun EUR(value: String): AmountDTO {
            return AmountDTO(value)
        }
    }
}