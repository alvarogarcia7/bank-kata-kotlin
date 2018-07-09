package com.example.kata.bank.service.domain.accounts

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class PinCodeTest {
    @Test
    fun `validated by the same pincode`() {
        assertThat(PinCode("1234").validatedBy(PinCode("1234"))).isTrue()
    }

    @Test
    fun `validated by a different pincode`() {
        assertThat(PinCode("1234").validatedBy(PinCode("0000"))).isFalse()
    }
}
