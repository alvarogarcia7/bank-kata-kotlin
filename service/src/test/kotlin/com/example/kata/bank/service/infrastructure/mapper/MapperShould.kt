package com.example.kata.bank.service.infrastructure.mapper

import com.example.kata.bank.service.domain.transactions.Amount
import com.example.kata.bank.service.domain.transactions.Transaction
import com.example.kata.bank.service.infrastructure.operations.AmountDTO
import com.example.kata.bank.service.infrastructure.operations.TimeDTO
import com.example.kata.bank.service.infrastructure.operations.TransactionDTO
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class MapperShould {
    @Test
    fun `map a deposit`() {
        val time = LocalDateTime.of(2018, 10, 12, 23, 59)
        val description = "description"
        val domain = Transaction.Deposit(Amount.Companion.of("21.01"), time, description)
        val dto = TransactionDTO(AmountDTO.EUR("21.01"), description, TimeDTO("2018-10-12 23:59:00", "2018-10-12T23:59:00"))
        Assertions.assertThat(Mapper().toDTO(domain)).isEqualTo(dto)
    }
}