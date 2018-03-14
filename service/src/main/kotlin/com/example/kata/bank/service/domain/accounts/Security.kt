package com.example.kata.bank.service.domain.accounts

abstract class Security {
    abstract fun generate(): String

    class TwoFactorAuthNotifier : Security() {
        override fun generate(): String {
            return "1234"
        }
    }
}