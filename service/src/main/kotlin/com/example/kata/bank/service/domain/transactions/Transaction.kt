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

    open class Transfer(override val amount: Amount, override val time: LocalDateTime, override val description: String) : Transaction(amount,
            time, description) {
        override fun subtotal(amount: Amount): Amount {
            return amount
        }

        data class TransferReceived(
                override val amount: Amount,
                override val time: LocalDateTime,
                override val description: String,
                val originAccount: Id
        ) : Transfer(amount, time, description)

        sealed class Outgoing(
                override val amount: Amount,
                override val time: LocalDateTime,
                override val description: String,
                open val to: Id
        ) : Transfer(amount, time, description) {
            override fun subtotal(amount: Amount): Amount {
                return amount
            }

            data class Request(
                    override val amount: Amount,
                    override val time: LocalDateTime,
                    override val description: String,
                    override val to: Id,
                    val code: String
            ) : Outgoing(amount, time, description, to)

            data class Emitted(
                    override val amount: Amount,
                    override val time: LocalDateTime,
                    override val description: String,
                    override val to: Id
            ) : Outgoing(amount, time, description, to)
        }

        data class Completed(override val amount: Amount, override val time: LocalDateTime, override val description: String, val from: Id, val to: Id) : Transfer(amount, time,
                description) {
            override fun subtotal(amount: Amount): Amount {
                return amount.add(this.amount)
            }
        }
    }
}