package com.example.kata.bank.service.domain.transactions

import com.example.kata.bank.service.domain.transfers.IncomingTransfer
import com.example.kata.bank.service.domain.transfers.OutgoingTransfer
import com.example.kata.bank.service.domain.transfers.TransferPayload
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

    class Transfer(override val tx: Tx, val payload: TransferPayload) : Transaction(tx) {
        override fun subtotal(amount: Amount): Amount {
            return amount
        }

        data class Request(val from: OutgoingTransfer, val to: IncomingTransfer, val request: Tx)

        data class OutgoingCompleted(override val tx: Tx) : Transaction(tx) {
            override fun subtotal(amount: Amount): Amount {
                return amount.subtract(tx.amount)
            }
        }

        data class IncomingCompleted(override val tx: Tx) : Transaction(tx) {
            override fun subtotal(amount: Amount): Amount {
                return amount.add(tx.amount)
            }
        }
    }

}