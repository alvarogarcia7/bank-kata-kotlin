package com.example.kata.bank.service.domain

import com.example.kata.bank.service.domain.accounts.Account
import com.example.kata.bank.service.domain.transactions.Transaction

sealed class AccountRequest {

    abstract fun <T> apply(account: Account): T

    class StatementRequest private constructor(val filter: (Transaction) -> Boolean) : AccountRequest() {
        companion object {
            fun filter(filter: (Transaction) -> Boolean): StatementRequest {
                return StatementRequest(filter)
            }

            fun all(): StatementRequest {
                return StatementRequest { _ -> true }
            }
        }
        override fun <T> apply(account: Account): T {
            return account.createStatement(this) as T
        }
    }

}