package com.example.kata.bank.service.infrastructure

import com.example.kata.bank.service.domain.Amount
import com.example.kata.bank.service.domain.Transaction
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

sealed class StatementLine(open val balance: Amount) {
    fun format(format: String): FormattedStatementLine {
        return this.toValues().applyTemplate(format)
    }

    abstract fun toValues(): StatementLineValues

    data class Description(val message: String, override var balance: Amount) : StatementLine(balance) {
        override fun toValues(): StatementLineValues {
            val values = mapOf("message" to message, "balance" to balance)
            return StatementLineValues(values)
        }
    }

    class StatementLineValues(val values: Map<String, Any>) {
        private val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/uuuu")

        fun applyTemplate(format: String): FormattedStatementLine {
            val line = format
                    .let { consumeMarkers(it) }
                    .let { removeAllUnusedMarkers(it) }
            return StatementLine.FormattedStatementLine(line)
        }

        private fun removeAllUnusedMarkers(format: String) = format.replace("%\\{\\w*}".toRegex(), "")

        private fun consumeMarkers(format: String): String {
            return values.entries.fold(format, { acc, entry ->
                acc.replace("%{${entry.key}}", xformat(entry.value))
            })
        }

        private fun xformat(value: Any): String {
            return when (value) {
                is LocalDateTime -> {
                    format(value)
                }
                is Amount -> {
                    format(value)
                }
                is String -> {
                    format(value)
                }
                else -> {
                    format(value)
                }
            }
        }

        private fun format(localDateTime: LocalDateTime) = localDateTime.format(this.formatter)
        private fun format(amount: Amount) = amount.formatted()
        private fun format(value: String) = value
        private fun format(any: Any): Nothing = throw IllegalArgumentException("can't format this field: " + any.javaClass)
    }

    data class Credit(val date: LocalDateTime, val description: String, val amount: Amount, override val balance: Amount) : StatementLine(balance) {
        override fun toValues(): StatementLineValues {
            val values = mapOf("date" to date, "credit" to amount, "balance" to balance)
            return StatementLineValues(values)
        }
    }

    data class Debit(val date: LocalDateTime, val description: String, val amount: Amount, override val balance: Amount) : StatementLine(balance) {
        override fun toValues(): StatementLineValues {
            val values = mapOf("date" to date, "debit" to amount, "balance" to balance)
            return StatementLineValues(values)
        }
    }

    companion object {
        fun `initial`(): StatementLine {
            return Description("previous balance", Amount.of("0"))
        }

        fun parse(transaction: Transaction, previousBalance: Amount): StatementLine {
            return when (transaction) {
                is Transaction.Deposit -> {
                    Credit(transaction.time, "", transaction.amount, previousBalance.add(transaction.amount))
                }
                is Transaction.Withdrawal -> {
                    Debit(transaction.time, "", transaction.amount, previousBalance.subtract(transaction.amount))
                }
            }
        }
    }


    data class FormattedStatementLine(val value: String)
}