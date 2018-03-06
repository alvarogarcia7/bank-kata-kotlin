package com.example.kata.bank.service.infrastructure.statement

import com.example.kata.bank.service.domain.Amount
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class StatementLineValues(val values: Map<String, Any>) {
    private val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/uuuu")

    fun applyTemplate(format: String): StatementLine.FormattedStatementLine {
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