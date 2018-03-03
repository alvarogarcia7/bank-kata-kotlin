package com.example.kata.bank.service.domain

import java.time.LocalDateTime

interface Clock {
    fun getTime(): LocalDateTime
}