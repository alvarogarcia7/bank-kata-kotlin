package com.example.kata.bank.service.infrastructure

import com.example.kata.bank.service.domain.Transaction

class Statement private constructor(val lines: List<StatementLine>) {
    companion object {
        fun including(
                initial: StatementLine,
                transactions: List<Transaction>): Statement {

            val initial1 = Pair(initial, mutableListOf<StatementLine>())
            val (_, statementLines) = transactions.reversed().foldRight(initial1,
                    { x, (y, z) ->
                        val element = StatementLine.parse(x, y.balance)
                        z.add(element)
                        Pair(element, z)
                    })
            statementLines.reverse()
            statementLines.add(initial)
            return Statement(statementLines)
        }
    }

}