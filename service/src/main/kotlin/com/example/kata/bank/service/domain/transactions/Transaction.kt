package com.example.kata.bank.service.domain.transactions

import java.time.LocalDateTime

sealed class Transaction(open val amount: Amount, open val time: LocalDateTime, open val description: String) {
    data class Deposit(override val amount: Amount, override val time: LocalDateTime, override val description: String) : Transaction(amount, time, description)
    data class Withdrawal(override val amount: Amount, override val time: LocalDateTime, override val description: String) : Transaction(amount, time, description)
}