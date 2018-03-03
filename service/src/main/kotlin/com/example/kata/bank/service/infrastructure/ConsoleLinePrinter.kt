package com.example.kata.bank.service.infrastructure

class ConsoleLinePrinter : LinePrinter {
    override fun println(line: String) {
        System.out.println(line)
    }

}