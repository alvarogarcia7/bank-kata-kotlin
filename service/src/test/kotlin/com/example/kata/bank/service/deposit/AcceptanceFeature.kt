package com.example.kata.bank.service.deposit

import com.example.kata.bank.service.domain.AccountRequest
import com.example.kata.bank.service.domain.FakeClock
import com.example.kata.bank.service.domain.accounts.Account
import com.example.kata.bank.service.domain.transactions.Amount
import com.example.kata.bank.service.infrastructure.statement.ConsoleLinePrinter
import com.example.kata.bank.service.infrastructure.statement.LinePrinter
import com.example.kata.bank.service.infrastructure.statement.StatementPrinter
import com.nhaarman.mockito_kotlin.mock
import org.junit.jupiter.api.Test
import org.junit.platform.runner.JUnitPlatform
import org.junit.runner.RunWith
import org.mockito.Mockito
import java.time.LocalDateTime


@RunWith(JUnitPlatform::class) // need to use this with infinitest
class AcceptanceFeature {

    @Test
    fun `operate with the account, then print statement`() {
        val clock = FakeClock.reading(
                    date("10/01/2012"),
                    date("13/01/2012"),
                    date("14/01/2012")

        )
        val mockLinePrinter = mock<LinePrinter> {}
        val decoratedLinePrinter = object : ConsoleLinePrinter() {
            override fun println(line: String) {
                super.println(line)
                mockLinePrinter.println(line)
            }
        }

        val account = Account(clock, "savings account #1")


        account.deposit(Amount.of("1000"), "from friend 1")
        account.deposit(Amount.of("2000"), "from friend 2")
        account.withdraw(Amount.of("500"), "for friend 3")
        val statement = account.createStatement(AccountRequest.StatementRequest.all())

        StatementPrinter(decoratedLinePrinter).print(statement)

        verifyPrintedLines(mockLinePrinter,
                "date || message || credit || debit || balance",
                "14/01/2012 ||  ||  || 1.00 || 2499.00",
                "14/01/2012 ||  ||  || 500.00 || 2500.00",
                "13/01/2012 ||  || 2000.00 ||  || 3000.00",
                "10/01/2012 ||  || 1000.00 ||  || 1000.00",
                " || previous balance ||  ||  || 0.00")
    }

    private fun date(value: String): LocalDateTime {
        return FakeClock.date(value)
    }

    private fun verifyPrintedLines(linePrinter: LinePrinter, vararg lines: String) {
        val inOrder = Mockito.inOrder(linePrinter)
        lines.map {
            inOrder.verify(linePrinter).println(it)
        }
        Mockito.verifyNoMoreInteractions(linePrinter)
    }


}

