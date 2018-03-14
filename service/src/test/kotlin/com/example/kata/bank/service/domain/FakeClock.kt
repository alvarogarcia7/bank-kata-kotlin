package com.example.kata.bank.service.domain

import com.example.kata.bank.service.domain.accounts.Clock
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class FakeClock(private val clock: Clock) : Clock {
    override fun getTime(): LocalDateTime {
        return clock.getTime()
    }

    companion object {
        private val dateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/uuuu" + " " + "HH:mm:ss")
        fun reading(vararg value: LocalDateTime): Clock {
            val clock = mock<Clock> {
                on { getTime() } doReturn value.toList()
            }
            return FakeClock(clock)
        }

        fun date(date: String): LocalDateTime {
            return LocalDateTime.parse(date + " " + "00:00:00", dateTimeFormatter)
        }
    }

}
