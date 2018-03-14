package com.example.kata.bank.service.domain.transactions

import com.example.kata.bank.service.domain.Id
import java.time.LocalDateTime

sealed class Transaction(open val amount: Amount, open val time: LocalDateTime, open val description: String) {
    abstract fun subtotal(amount: Amount): Amount

    data class Deposit(override val amount: Amount, override val time: LocalDateTime, override val description: String) : Transaction(amount, time, description) {
        override fun subtotal(amount: Amount): Amount {
            return amount.add(this.amount)
        }
    }

    data class Withdrawal(override val amount: Amount, override val time: LocalDateTime, override val description: String) : Transaction(amount, time, description) {
        override fun subtotal(amount: Amount): Amount {
            return amount.subtract(this.amount)
        }
    }

    data class Cost(override val amount: Amount, override val time: LocalDateTime, override val description: String) : Transaction(amount, time, description) {
        override fun subtotal(amount: Amount): Amount {
            return amount.subtract(this.amount)
        }
    }

    data class TransferReceived(override val amount: Amount, override val time: LocalDateTime, override val description: String, val originAccount: Id) : Transaction(amount, time,
            description) {
        override fun subtotal(amount: Amount): Amount {
            return amount.add(this.amount)
        }
    }

    data class TransferEmitted(override val amount: Amount, override val time: LocalDateTime, override val description: String, val destinationAccount: Id) : Transaction(amount,
            time, description) {
        override fun subtotal(amount: Amount): Amount {
            return amount.subtract(this.amount)
        }
    }

    data class Transfer(override val amount: Amount, override val time: LocalDateTime, override val description: String,
                        val originAccount: Id,
                        val destinationAccount: Id) : Transaction(amount,
            time, description) {
        override fun subtotal(amount: Amount): Amount {
            return amount
        }
    }
}