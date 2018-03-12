package com.example.kata.bank.service.infrastructure.statement

import com.example.kata.bank.service.domain.transactions.Amount
import com.example.kata.bank.service.domain.transactions.Transaction
import java.time.LocalDateTime

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
                is Transaction.Cost -> {
                    Debit(transaction.time, "", transaction.amount, previousBalance.subtract(transaction.amount))
                }
            }
        }
    }


    data class FormattedStatementLine(val value: String)
}