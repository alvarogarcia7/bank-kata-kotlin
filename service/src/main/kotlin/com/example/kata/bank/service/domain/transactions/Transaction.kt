package com.example.kata.bank.service.domain.transactions

import arrow.core.Option
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

        /**
         *
        2018-03-24 20:29:30 AGB
        ## State Machine initial - wrong

        ### workflow inseguro
        inicial -> completado [final]

        ### workflow seguro
        initial          -> completed
        -> denied

        initial -LAMBDA-> waiting outgoing confirmation State
        waiting outgoing confirmation State -confirmed-> incoming request State
        -denied-> deniedState [final]

        incoming request State -LAMBDA-> waiting incoming request State

        waiting incoming confirmation State -confirmed-> completed State [final]
        -denied-> deniedState [final]

        2018-03-24 20:29:44 AGB
        ## State machine 2

        ### workflow all together
        initial -outgoing enabled?-> waiting outgoing confirmation State
        -outgoing disable?-> incoming request State

        waiting outgoing confirmation State -confirmed-> incoming request State
        -denied-> deniedState [final]

        incoming request State -incoming enabled?-> waiting incoming confirmation State
        -incoming disable?-> confirmed

        waiting incoming confirmation State -confirmed-> completed State [final]
        -denied-> deniedState [final]

         */
        data class Workflow private constructor(
                val pendingParts: List<IRequest>,
                val toConfirmWhenEverythingIsReady: List<Pair<Account, Id>>
        ) {

            companion object {
                fun from(pendingParts: List<IRequest>,
                         toConfirmWhenEverythingIsReady: List<Pair<Account, Id>>): Workflow {
                    return Workflow(pendingParts, toConfirmWhenEverythingIsReady)
                }
            }

            fun confirm(code: String): Option<Workflow> {
                if (pendingParts.isNotEmpty()) {
                    val outgoing = this.pendingParts.first()

                    if (outgoing.`unlockedBy?`(code)) {
                        val incomingConfirmation = this.pendingParts[1]
                        if (incomingConfirmation.isEmpty()) {
                            toConfirmWhenEverythingIsReady.map { (account, transaction) -> account.confirm(transaction) }
                            return Option.empty()
                        }
                        return Option(Workflow(incomingConfirmation, this.toConfirmWhenEverythingIsReady))
                    }
                }
                return Option(this)
            }
        }

        interface IRequest {
            fun `unlockedBy?`(code: String): Boolean
        }

        data class SecureRequest(override val tx: Tx, private val code: String, val transfer: Transfer) : Transfer(tx), IRequest {
            override fun blocked() = true
            override fun subtotal(amount: Amount) = amount

            override fun `unlockedBy?`(code: String): Boolean {
                return code == this.code
            }
        }

        data class InsecureRequest(override val tx: Tx, val transfer: Transfer) : Transfer(tx), IRequest {
            override fun blocked() = true
            override fun subtotal(amount: Amount) = amount

            override fun `unlockedBy?`(code: String): Boolean {
                return true
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