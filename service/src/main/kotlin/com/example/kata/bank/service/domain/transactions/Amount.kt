package com.example.kata.bank.service.domain.transactions

import java.math.BigDecimal
import java.text.DecimalFormat

data class Amount private constructor(private val value: BigDecimal) {
    companion object {
        fun `of`(value: String): Amount {
            return `ofBigDecimal`(BigDecimal(value))
        }

        private fun `ofBigDecimal`(value: BigDecimal): Amount {
            return Amount(value)
        }
    }

    fun add(amount: Amount): Amount {
        return `ofBigDecimal`(this.value.add(amount.value))
    }

    fun subtract(amount: Amount): Amount {
        return `ofBigDecimal`(this.value.subtract(amount.value))
    }

    fun formatted(): String {
        return DecimalFormat("0.00").format(this.value)
    }

    fun greaterThan(other: Amount): Boolean {
        return this.value > other.value
    }
}