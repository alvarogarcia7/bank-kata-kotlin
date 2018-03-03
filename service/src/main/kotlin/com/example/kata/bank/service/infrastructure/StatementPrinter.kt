package com.example.kata.bank.service.infrastructure

class StatementPrinter(val linePrinter: LinePrinter) {
    fun print(statement: Statement) {
        linePrinter.println("date || credit || debit || balance")
        statement.lines.map { it.format() }.map { linePrinter.println(it.value) }

    }

}