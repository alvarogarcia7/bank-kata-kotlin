package com.example.kata.bank.service.deposit

import com.example.kata.bank.service.domain.Account
import com.example.kata.bank.service.domain.Amount
import com.example.kata.bank.service.domain.Clock
import com.example.kata.bank.service.infrastructure.LinePrinter
import com.example.kata.bank.service.infrastructure.StatementPrinter
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import org.junit.jupiter.api.Test
import org.junit.platform.runner.JUnitPlatform
import org.junit.runner.RunWith
import org.mockito.Mockito
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
//        val linePrinter = ConsoleLinePrinter()
        val account = Account(clock)


        account.deposit(Amount.of("1000"), "from friend 1")
        account.deposit(Amount.of("2000"), "from friend 2")
        account.withdraw(Amount.of("500"), "for friend 3")
        account.printStatement(StatementPrinter(linePrinter
//                ,
//                StatementLine.StatementLineFormatter.of("{{date}} || {{credit}} || {{debit}} || {{balance}}"))
        ))

        verifyPrintedLines(linePrinter,
                "date || credit || debit || balance",
                "14/01/2012 ||  || 500.00 || 2500.00",
                "13/01/2012 || 2000.00 ||  || 3000.00",
                "10/01/2012 || 1000.00 ||  || 1000.00")
    }

    private fun date(value: String): LocalDateTime {
        val dateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/uuuu" + " " + "HH:mm:ss")
        return LocalDateTime.parse(value + " " + "00:00:00", dateTimeFormatter)
    }

    private fun verifyPrintedLines(linePrinter: LinePrinter, vararg lines: String) {
        val inOrder = Mockito.inOrder(linePrinter)
        lines.map {
            inOrder.verify(linePrinter).println(it)
        }
    }


}

