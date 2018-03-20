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

    abstract class Transfer(override val tx: Tx) : Transaction(tx) {

        override fun subtotal(amount: Amount): Amount {
            return amount
        }

        data class Chain(override val tx: Tx, val t1: Request, val next: Chain? = null) : Transfer(tx)

        sealed class Request(open val from: Persisted<Account>, open val destination: Persisted<Account>) {
            abstract fun unlockedBy(code: String): Boolean

            data class Request(override val from: Persisted<Account>, override val destination: Persisted<Account>, private val code: String) : Transfer.Request(from, destination) {
                override fun unlockedBy(code: String): Boolean {
                    return code == this.code
                }
            }
        }

        data class Completed(val from: Id, val to: Id)

        data class Emitted(override val tx: Tx, val completed: Completed) : Transfer(tx) {
            override fun subtotal(amount: Amount): Amount {
                return amount.subtract(this.tx.amount)
            }
        }

//        data class Intermediate(override val tx: Tx, val request: Request, val next: Intermediate? = null) : Transfer(tx)

        data class Received(override val tx: Tx, val completed: Completed) : Transfer(tx) {
            override fun subtotal(amount: Amount): Amount {
                return amount.add(this.tx.amount)
            }
        }
    }
}