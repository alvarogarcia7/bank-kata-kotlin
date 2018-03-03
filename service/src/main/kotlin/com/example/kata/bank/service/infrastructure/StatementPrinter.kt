package com.example.kata.bank.service.infrastructure

class StatementPrinter(val linePrinter: LinePrinter) {
    fun print(statement: Statement) {
        linePrinter.println("date || message || credit || debit || balance")
        statement.lines
                .map { it.format("%{date} || %{message} || %{credit} || %{debit} || %{balance}") }
                .map { linePrinter.println(it.value) }

    }

}