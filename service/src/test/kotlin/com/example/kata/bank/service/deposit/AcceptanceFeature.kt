package com.example.kata.bank.service.deposit

import com.example.kata.bank.service.deposit.AcceptanceFeature.Account.Amount
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import org.junit.jupiter.api.Test
import org.junit.platform.runner.JUnitPlatform
import org.junit.runner.RunWith
import org.mockito.Mockito
import java.math.BigDecimal
import java.text.DecimalFormat
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

    class Account(val clock: Clock) {
        private val transactionRepository: TransactionRepository = TransactionRepository()

        fun deposit(amount: Amount, description: String) {
            this.transactionRepository.save(Transaction.Deposit(amount, clock.getTime(), description))
        }

        data class Amount private constructor(private val value: BigDecimal) {
            companion object {
                fun `of`(value: String): Amount {
                    return of(BigDecimal(value))
                }

                private fun `of`(value: BigDecimal): Amount {
                    return Amount(value)
                }
            }

            fun add(amount: Amount): Amount {
                return of(this.value.add(amount.value))
            }

            fun subtract(amount: Amount): Amount {
                return of(this.value.subtract(amount.value))
            }

            fun formatted(): String {
                return DecimalFormat("#.00").format(this.value)
            }
        }

        fun withdraw(amount: Amount, description: String) {
            this.transactionRepository.save(Transaction.Withdrawal(amount, clock.getTime(), description))
        }

        fun printStatement(statementPrinter: StatementPrinter) {
            statementPrinter.print(Statement.including(StatementLine.initial(), transactionRepository.findAll()))
        }

    }

    class Statement private constructor(val lines: List<StatementLine>) {
        companion object {
            fun including(
                    initial: StatementLine,
                    transactions: List<Transaction>): Statement {

                val initial1 = Pair(initial, mutableListOf<StatementLine>())
                val (_, lines) = transactions.foldRight(initial1,
                        { x, (y, z) ->
                            val element = StatementLine.parse(x, y.balance)
                            z.add(0, element)
                            Pair(element, z)
                        })
                return Statement(lines)
            }
        }

    }

    sealed class StatementLine(open val balance: Amount) {
        protected val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/uuuu")
        protected fun format(localDateTime: LocalDateTime) = localDateTime.format(this.formatter)


        data class Description(val message: String, override var balance: Amount) : StatementLine(balance) {
            override fun format(): FormattedStatementLine {
                return FormattedStatementLine("${message} || ${balance}")
            }
        }

        data class Credit(val date: LocalDateTime, val description: String, val credit: Amount, override val balance: Amount) : StatementLine(balance) {
            override fun format(): FormattedStatementLine {
                val formattedDate = format(date)
                val formattedBalance = balance.formatted()
                val formattedAmount = credit.formatted()
                return FormattedStatementLine("$formattedDate ||  || ${formattedAmount} || ${formattedBalance}")
            }

        }

        data class Debit(val date: LocalDateTime, val description: String, val debit: Amount, override val balance: Amount) : StatementLine(balance) {
            override fun format(): FormattedStatementLine {
                val formattedDate = format(date)
                val formattedBalance = balance.formatted()
                val formattedAmount = debit.formatted()
                return FormattedStatementLine("$formattedDate || ${formattedAmount} ||  || ${formattedBalance}")
            }
        }

        companion object {
            fun `initial`(): StatementLine {
                return Description("Previous balance:", Amount.of("0"))
            }

            fun parse(transaction: Transaction, previousBalance: Amount): StatementLine {
                return when (transaction) {
                    is Transaction.Deposit -> {
                        Debit(transaction.time, "", transaction.amount, previousBalance.add(transaction.amount))
                    }
                    is Transaction.Withdrawal -> {
                        Credit(transaction.time, "", transaction.amount, previousBalance.subtract(transaction.amount))
                    }
                }
            }
        }

        abstract fun format(): FormattedStatementLine

        data class FormattedStatementLine(val value: String)

//        abstract class StatementLineFormatter {
//            fun format(line: StatementLine) {
//                return line.format()
//            }
//
//            companion object {
//                fun of(definition: String): StatementLineFormatter {
//                    return onlyKind()
//                }
//
//                private fun onlyKind(): StatementLineFormatter {
//                }
//            }
//
//        }

    }


    sealed class Transaction(open val amount: Amount, open val time: LocalDateTime, open val description: String) {
        data class Deposit(override val amount: Amount, override val time: LocalDateTime, override val description: String) : Transaction(amount, time, description)
        data class Withdrawal(override val amount: Amount, override val time: LocalDateTime, override val description: String) : Transaction(amount, time, description)
    }

    class TransactionRepository {
        private val values = mutableListOf<Transaction>()
        fun save(transaction: Transaction) {
            values.add(0, transaction)
        }

        fun findAll(): List<Transaction> {
            return values.toList()
        }

    }
}

class ConsoleLinePrinter : LinePrinter {
    override fun println(line: String) {
        System.out.println(line)
    }

}

class StatementPrinter(val linePrinter: LinePrinter) {
    fun print(statement: AcceptanceFeature.Statement) {
        linePrinter.println("date || credit || debit || balance")
        statement.lines.map { it.format() }.map { linePrinter.println(it.value) }

    }

}

interface LinePrinter {
    fun println(line: String)
}

interface Clock {
    fun getTime(): LocalDateTime
}
