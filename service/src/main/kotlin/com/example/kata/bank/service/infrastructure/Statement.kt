package com.example.kata.bank.service.infrastructure

import com.example.kata.bank.service.domain.Transaction

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
            return Statement(lines.union(listOf(initial)).toList())
        }
    }

}