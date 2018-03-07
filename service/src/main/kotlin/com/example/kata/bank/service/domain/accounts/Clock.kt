package com.example.kata.bank.service.domain.accounts

import java.time.LocalDateTime

interface Clock {
    companion object {
        fun aNew(): Clock {
            return object : Clock {
                override fun getTime(): LocalDateTime {
                    return LocalDateTime.now()
                }
            }
        }
    }

    fun getTime(): LocalDateTime
}

