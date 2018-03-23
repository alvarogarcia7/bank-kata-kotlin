package com.example.kata.bank.service.domain.transactions

import arrow.core.Either
import arrow.core.Option
import com.example.kata.bank.service.NotTestedOperation
import com.example.kata.bank.service.domain.Id
import com.example.kata.bank.service.domain.accounts.Account
import java.time.LocalDateTime


data class Tx(val amount: Amount, val time: LocalDateTime, val description: String)


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

        data class Workflow private constructor(
                val pendingParts: List<Either<SecureRequest, Transfer>>,
                val toConfirmWhenEverythingIsReady: List<Pair<Account, Id>>
        ) {

            companion object {
                fun from(pendingParts: List<Either<SecureRequest, Transfer>>,
                         toConfirmWhenEverythingIsReady: List<Pair<Account, Id>>): Workflow {
                    return Workflow(pendingParts.filter { it.isLeft() }, toConfirmWhenEverythingIsReady)
                }
            }

            fun confirm(code: String): Option<Workflow> {
                if (pendingParts.isNotEmpty()) {
                    val x = this.pendingParts.first().mapLeft { it.unlockedBy(code) }
                    val result = when (x) {
                        is Either.Left -> {
                            if (x.a) {
                                true
                            } else {
                                throw NotTestedOperation()
                            }
                        }
                        is Either.Right -> {
                            true
                        }
                    }
                    if (result) {
                        val remainingParts = this.pendingParts.subList(1, this.pendingParts.size)
                        if (remainingParts.isEmpty()) {
                            toConfirmWhenEverythingIsReady.map { (account, transaction) -> account.confirm(transaction) }
                            return Option.empty()
                        }
                        return Option(Workflow(remainingParts, this.toConfirmWhenEverythingIsReady))
                    }
                }
                return Option(this)
            }
        }

        data class SecureRequest(override val tx: Tx, private val code: String, val transfer: Transfer) : Transfer(tx) {
            override fun blocked() = true
            override fun subtotal(amount: Amount) = amount

            fun unlockedBy(code: String): Boolean {
                return code == this.code
            }
        }

        data class InsecureRequest(override val tx: Tx, val transfer: Transfer) : Transfer(tx) {
            override fun blocked() = true
            override fun subtotal(amount: Amount) = amount
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