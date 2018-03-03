package com.example.kata.bank.service.infrastructure

import com.example.kata.bank.service.domain.Amount
import com.example.kata.bank.service.domain.Transaction
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

sealed class StatementLine(open val balance: Amount) {
    protected val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/uuuu")
    protected fun format(localDateTime: LocalDateTime) = localDateTime.format(this.formatter)


    data class Description(val message: String, override var balance: Amount) : StatementLine(balance) {
        override fun format(): FormattedStatementLine {
            return FormattedStatementLine("${message} || ${balance}")
        }
    }

    data class Credit(val date: LocalDateTime, val description: String, val credit: Amount, override val balance: Amount) : StatementLine(balance) {
        override fun format(): FormattedStatementLine {
            val formattedDate = format(date)
            val formattedBalance = balance.formatted()
            val formattedAmount = credit.formatted()
            return FormattedStatementLine("$formattedDate ||  || ${formattedAmount} || ${formattedBalance}")
        }

    }

    data class Debit(val date: LocalDateTime, val description: String, val debit: Amount, override val balance: Amount) : StatementLine(balance) {
        override fun format(): FormattedStatementLine {
            val formattedDate = format(date)
            val formattedBalance = balance.formatted()
            val formattedAmount = debit.formatted()
            return FormattedStatementLine("$formattedDate || ${formattedAmount} ||  || ${formattedBalance}")
        }
    }

    companion object {
        fun `initial`(): StatementLine {
            return Description("Previous balance:", Amount.of("0"))
        }

        fun parse(transaction: Transaction, previousBalance: Amount): StatementLine {
            return when (transaction) {
                is Transaction.Deposit -> {
                    Debit(transaction.time, "", transaction.amount, previousBalance.add(transaction.amount))
                }
                is Transaction.Withdrawal -> {
                    Credit(transaction.time, "", transaction.amount, previousBalance.subtract(transaction.amount))
                }
            }
        }
    }

    abstract fun format(): FormattedStatementLine

    data class FormattedStatementLine(val value: String)

//        abstract class StatementLineFormatter {
//            fun format(line: StatementLine) {
//                return line.format()
//            }
//
//            companion object {
//                fun of(definition: String): StatementLineFormatter {
//                    return onlyKind()
//                }
//
//                private fun onlyKind(): StatementLineFormatter {
//                }
//            }
//
//        }

}