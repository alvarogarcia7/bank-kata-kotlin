package com.example.kata.bank.service.deposit

import com.example.kata.bank.service.deposit.AcceptanceFeature.Account.Amount
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import org.junit.jupiter.api.Test
import org.junit.platform.runner.JUnitPlatform
import org.junit.runner.RunWith
import org.mockito.Mockito.inOrder
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


@RunWith(JUnitPlatform::class) // need to use this with infinitest
class AcceptanceFeature {

    @Test
    fun `operate with the account, then print statement`() {
        val clock = mock<Clock> {
            on { getTime() } doReturn listOf(
                    date("10/01/2012"),
                    date("13/01/2012"),
                    date("14/01/2012")
            )
        }
        val linePrinter = mock<LinePrinter> { }
        val account = Account(clock)


        account.deposit(Amount("1000"))
        account.deposit(Amount("2000"))
        account.withdraw(Amount("500"))
        account.printStatement(StatementPrinter(linePrinter))

        verifyPrintedLines(linePrinter,
                "date || credit || debit || balance",
                "14/01/2012 || || 500.00 || 2500.00",
                "13/01/2012 || 2000.00 || || 3000.00",
                "10/01/2012 || 1000.00 || || 1000.00")
    }

    private fun date(value: String): LocalDateTime {
        val dateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/uuuu" + " " + "HH:mm:ss")
        return LocalDateTime.parse(value + " " + "00:00:00", dateTimeFormatter)
    }

    private fun verifyPrintedLines(linePrinter: LinePrinter, vararg lines: String) {
        //create inOrder object passing any mocks that need to be verified in order
        val inOrder = inOrder(linePrinter)
        lines.map {
            inOrder.verify(linePrinter).println(it)
        }
    }

    class Account(val clock: Clock) {
        fun deposit(amount: Amount) {
        }

        data class Amount(val value: String)

        fun withdraw(amount: Amount) {
        }

        fun printStatement(statementPrinter: StatementPrinter) {
        }

    }
}

class StatementPrinter(linePrinter: LinePrinter) {

}

interface LinePrinter {
    fun println(line: String)
}

interface Clock {
    fun getTime(): LocalDateTime
}
