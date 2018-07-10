package com.example.kata.bank.service.infrastructure.statement

import com.example.kata.bank.service.domain.transactions.Amount
import com.example.kata.bank.service.domain.transactions.Transaction
import com.example.kata.bank.service.domain.transactions.Tx
import java.time.LocalDateTime

data class StatementLine(val tx: Tx, val balance: Amount) {

    companion object {
        fun `initial`(): StatementLine {
            val amount = Amount.of("0")
            return StatementLine(Tx(amount, LocalDateTime.MIN, "Initial balance"), amount)
        }

        fun parse(current: Transaction, balance: Amount): StatementLine {
            return StatementLine(current.tx, current.subtotal(balance))
        }
    }
}