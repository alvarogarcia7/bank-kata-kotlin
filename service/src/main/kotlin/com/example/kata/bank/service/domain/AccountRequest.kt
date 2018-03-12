package com.example.kata.bank.service.domain

import com.example.kata.bank.service.domain.accounts.Account
import com.example.kata.bank.service.domain.transactions.Transaction

sealed class AccountRequest {

    abstract fun <T> apply(account: Account): T

    class StatementRequest : AccountRequest() {
        override fun <T> apply(account: Account): T {
            return account.createStatement() as T
        }

        private val filter: (Transaction) -> Boolean = { _ -> true }
    }

}