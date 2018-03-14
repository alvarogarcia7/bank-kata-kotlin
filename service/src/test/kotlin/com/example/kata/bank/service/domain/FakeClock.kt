package com.example.kata.bank.service.domain

import com.example.kata.bank.service.domain.accounts.Clock
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import java.time.LocalDateTime

class FakeClock(private val clock: Clock) : Clock {
    override fun getTime(): LocalDateTime {
        return clock.getTime()
    }

    companion object {
        fun reading(vararg value: LocalDateTime): Clock {
            val clock = mock<Clock> {
                on { getTime() } doReturn value[0]
            }
            return FakeClock(clock)
        }
    }

}
