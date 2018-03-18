package com.example.kata.bank.service.domain.transactions

import com.example.kata.bank.service.domain.Id
import com.example.kata.bank.service.domain.Persisted
import com.example.kata.bank.service.domain.accounts.Account
import java.time.LocalDateTime


data class Tx(open val amount: Amount, open val time: LocalDateTime, open val description: String)


sealed class Transaction(open val tx: Tx) {
    abstract fun subtotal(amount: Amount): Amount

    data class Deposit(override val tx: Tx) : Transaction(tx) {
        override fun subtotal(amount: Amount): Amount {
            return amount.add(this.tx.amount)
        }
    }

    data class Withdrawal(override val tx: Tx) : Transaction(tx) {
        override fun subtotal(amount: Amount): Amount {
            return amount.subtract(this.tx.amount)
        }
    }

    data class Cost(override val tx: Tx) : Transaction(tx) {
        override fun subtotal(amount: Amount): Amount {
            return amount.subtract(this.tx.amount)
        }
    }

    open class Transfer(override val tx: Tx) : Transaction(tx) {
        override fun subtotal(amount: Amount): Amount {
            return amount
        }

        data class Request(val from: Persisted<Account>, val destination: Persisted<Account>, private val code: String)

        data class Completed(val from: Id, val to: Id)

        data class Emitted(override val tx: Tx, val completed: Completed) : Transfer(tx) {
            override fun subtotal(amount: Amount): Amount {
                return amount.subtract(this.tx.amount)
            }
        }

        data class Intermediate(override val tx: Tx, val request: Request) : Transfer(tx)

        data class Received(override val tx: Tx, val completed: Completed) : Transfer(tx) {
            override fun subtotal(amount: Amount): Amount {
                return amount.add(this.tx.amount)
            }
        }
    }
}