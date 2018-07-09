package com.example.kata.bank.service.domain.accounts

abstract class Security {
    abstract fun generate(): PinCode

    class TwoFactorAuthNotifier : Security() {
        override fun generate(): PinCode {
            return PinCode("1234")
        }
    }
}

data class PinCode(val value: String) {
    fun validatedBy(code: PinCode): Boolean {
        return this == code
    }
}
