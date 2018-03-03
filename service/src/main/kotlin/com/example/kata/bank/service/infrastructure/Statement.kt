package com.example.kata.bank.service.infrastructure

import com.example.kata.bank.service.domain.Transaction

class Statement private constructor(val lines: List<StatementLine>) {
    companion object {
        fun including(
                initialStatement: StatementLine,
                transactions: List<Transaction>): Statement {

            val initial = Pair(initialStatement, mutableListOf<StatementLine>())
            val (_, statementLines) = transactions.reversed().foldRight(initial,
                    { current, (previousStatement, result) ->
                        val currentStatement = StatementLine.parse(current, previousStatement.balance)
                        result.add(currentStatement)
                        Pair(currentStatement, result)
                    })
            statementLines.reverse()
            statementLines.add(initialStatement)
            return Statement(statementLines)
        }
    }

}