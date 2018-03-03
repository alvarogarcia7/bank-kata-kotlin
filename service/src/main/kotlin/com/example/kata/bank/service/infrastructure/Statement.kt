package com.example.kata.bank.service.infrastructure

class Statement private constructor(val lines: List<StatementLine>) {
    companion object {
        fun inReverseOrder(statementLines: List<StatementLine>): Statement {
            return Statement(statementLines.reversed())
        }
    }

}