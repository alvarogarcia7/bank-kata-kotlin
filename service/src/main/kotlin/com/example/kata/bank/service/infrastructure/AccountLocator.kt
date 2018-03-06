package com.example.kata.bank.service.infrastructure

import arrow.core.Option
import com.example.kata.bank.service.domain.Account
import com.example.kata.bank.service.domain.Clock
import com.example.kata.bank.service.domain.UserId

class AccountLocator {
    companion object {
        private val accounts = mapOf(UserId("1234") to Account(Clock.aNew()))
        fun `for`(userId: UserId): Option<Account> {
            return Option.fromNullable(accounts[userId])
        }
    }

}