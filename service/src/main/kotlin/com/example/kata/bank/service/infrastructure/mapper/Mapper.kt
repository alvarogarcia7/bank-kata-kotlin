package com.example.kata.bank.service.infrastructure.mapper

import com.example.kata.bank.service.domain.accounts.Account
import com.example.kata.bank.service.domain.transactions.Amount
import com.example.kata.bank.service.domain.transactions.Transaction
import com.example.kata.bank.service.domain.users.User
import com.example.kata.bank.service.infrastructure.accounts.AccountDTO
import com.example.kata.bank.service.infrastructure.operations.AmountDTO
import com.example.kata.bank.service.infrastructure.operations.TimeDTO
import com.example.kata.bank.service.infrastructure.operations.TransactionDTO
import com.example.kata.bank.service.infrastructure.users.UserDTO
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class Mapper {
    private val humanely = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss")
    private val iso = DateTimeFormatter.ISO_DATE_TIME
    fun toDTO(value: Transaction): TransactionDTO {
        return TransactionDTO(
                amount = toDTO(value.tx.amount),
                description = value.tx.description,
                time = toDTO(value.tx.time),
                type = "deposit")
    }

    private fun toDTO(time: LocalDateTime): TimeDTO {
        return TimeDTO(locale = humanely.format(time), iso = iso.format(time))
    }

    private fun toDTO(value: Amount): AmountDTO {
        return AmountDTO.EUR(value.formatted())
    }

    fun toDTO(account: Account): AccountDTO {
        return AccountDTO(account.name)
    }

    fun toDTO(user: User): UserDTO {
        return UserDTO(user.name)
    }

}

