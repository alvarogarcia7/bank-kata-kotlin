package com.example.kata.bank.service.test

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.platform.runner.JUnitPlatform
import org.junit.runner.RunWith

@RunWith(JUnitPlatform::class) // need to use this with infinitest
class CanaryTest {
    @Test
    fun `run`() {
        assertThat(true).isTrue()
    }
}