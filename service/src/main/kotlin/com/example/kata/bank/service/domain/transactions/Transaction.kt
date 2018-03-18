package com.example.kata.bank.service.domain.transactions

import arrow.core.Either
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
        abstract fun blocked(): Boolean
        override fun subtotal(amount: Amount): Amount {
            return amount
        }

        sealed class Request(open val from: Persisted<Account>, open val destination: Persisted<Account>) {
            abstract fun unlockedBy(code: String): Boolean

            data class Request(override val from: Persisted<Account>, override val destination: Persisted<Account>, private val code: String) : Transfer.Request(from, destination) {
                override fun unlockedBy(code: String): Boolean {
                    return code == this.code
                }
            }

            data class Recursive(override val from: Persisted<Account>, override val destination: Persisted<Account>, val re: Transfer, private val code: String) :
                    Transfer.Request(from, destination) {
                override fun unlockedBy(code: String): Boolean {
                    return code == this.code
                }
            }
        }

        data class Completed(val from: Id, val to: Id)

        data class Emitted(override val tx: Tx, val completed: Completed) : Transfer(tx) {
            override fun blocked(): Boolean {
                return false
            }

            override fun subtotal(amount: Amount): Amount {
                return amount.subtract(this.tx.amount)
            }
        }

        data class Intermediate(override val tx: Tx, val request: Request) : Transfer(tx) {
            fun unlock(code: String): Either<Request, Completed> {
                if (request.unlockedBy(code)) {
                    when (request) {
                        is Request.Request -> {
                            return Either.right(Completed(request.from.id, request.destination.id))
                        }
                        is Request.Recursive -> {
                            return Either.left(request.re as Request)
                        }
                    }
                } else {
                    return Either.left(request)
                }
            }

            override fun blocked(): Boolean {
                return true
            }
        }

        data class Received(override val tx: Tx, val completed: Completed) : Transfer(tx) {
            override fun blocked(): Boolean {
                return false
            }

            override fun subtotal(amount: Amount): Amount {
                return amount.add(this.tx.amount)
            }
        }
    }
}