package com.example.kata.bank.service.domain.transactions

import com.example.kata.bank.service.domain.Id
import com.example.kata.bank.service.domain.Persisted
import com.example.kata.bank.service.domain.accounts.Account
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

        sealed class Outgoing(
                override val amount: Amount,
                override val time: LocalDateTime,
                override val description: String
        ) : Transfer(amount, time, description) {

            override fun subtotal(amount: Amount): Amount {
                return amount
            }
            data class Request(
                    override val amount: Amount,
                    override val time: LocalDateTime,
                    override val description: String,
                    val from: Persisted<Account>,
                    val destination: Persisted<Account>,
                    private val code: String
            ) : Outgoing(amount, time, description)

            data class Emitted(
                    override val amount: Amount,
                    override val time: LocalDateTime,
                    override val description: String,
                    val from: Id,
                    val to: Id
            ) : Outgoing(amount, time, description) {
                override fun subtotal(amount: Amount): Amount {
                    return amount.subtract(this.amount)
                }
            }

        }

        sealed class Incoming(
                override val amount: Amount,
                override val time: LocalDateTime,
                override val description: String
        ) : Transfer(amount, time, description) {

            data class Request(
                    override val amount: Amount,
                    override val time: LocalDateTime,
                    override val description: String,
                    val from: Persisted<Account>,
                    val destination: Persisted<Account>,
                    private val code: String
            ) : Incoming(amount, time, description)

            data class Received(
                    override val amount: Amount,
                    override val time: LocalDateTime,
                    override val description: String,
                    val from: Id,
                    val to: Id
            ) : Transfer.Incoming(amount, time, description) {
                override fun subtotal(amount: Amount): Amount {
                    return amount.add(this.amount)
                }
            }
        }
    }
}