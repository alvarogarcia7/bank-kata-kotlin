package com.example.kata.bank.service.delivery

import com.example.kata.bank.service.domain.Account
import com.example.kata.bank.service.domain.Amount
import com.example.kata.bank.service.domain.Transaction
import com.example.kata.bank.service.domain.User
import com.example.kata.bank.service.infrastructure.operations.AmountDTO
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class Mapper {
    private val humanely = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss")
    private val iso = DateTimeFormatter.ISO_DATE_TIME
    fun toDTO(value: Transaction): TransactionDTO {
        return TransactionDTO(
                amount = toDTO(value.amount),
                description = value.description,
                time = toDTO(value.time))
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

data class AccountDTO(val name: String)
data class UserDTO(val name: String)

data class TimeDTO(val locale: String, val iso: String)

data class TransactionDTO(val amount: AmountDTO, val description: String, val time: TimeDTO)
