package com.example.kata.bank.service.domain.accounts

import arrow.core.Either
import arrow.core.None
import arrow.core.Option
import com.example.kata.bank.service.domain.AccountRequest
import com.example.kata.bank.service.domain.Id
import com.example.kata.bank.service.domain.Persisted
import com.example.kata.bank.service.domain.transactions.Amount
import com.example.kata.bank.service.domain.transactions.Transaction
import com.example.kata.bank.service.domain.transactions.Transaction.Transfer.*
import com.example.kata.bank.service.domain.transactions.TransactionRepository
import com.example.kata.bank.service.domain.transactions.Tx
import com.example.kata.bank.service.infrastructure.statement.Statement
import com.example.kata.bank.service.infrastructure.statement.StatementLine

class Account(
        val clock: Clock,
        val name: String,
        val type: AccountType = AccountType.Personal,
        private val incomingSecurity: Option<Security> = None,
        private val outgoingSecurity: Option<Security> = None
) {

    private val transactionRepository: TransactionRepository = TransactionRepository()

    fun deposit(amount: Amount, description: String): Id {
        val transaction = createIdentityFor(Transaction.Deposit(Tx(amount, clock.getTime(), description)))
        this.transactionRepository.save(transaction)
        return transaction.id
    }

    @Synchronized
    fun withdraw(operationAmount: Amount, description: String): Either<List<Exception>, Id> {
        if (type == AccountType.Personal) {
            if (operationAmount.greaterThan(balance())) {
                return Either.left(listOf(Exception("Cannot go overdraft")))
            }
        }
        val transaction = createIdentityFor(Transaction.Withdrawal(Tx(operationAmount, clock.getTime(), description)))
        this.transactionRepository.save(transaction)
        return Either.right(transaction.id)
    }

    private fun <T> createIdentityFor(value: T): Persisted<T> {
        return Persisted.`for`(value, Id.random())
    }

    fun createStatement(statementRequest: AccountRequest.StatementRequest): Statement {
        type.determineStatementCost(transactionRepository.findAll().map { it.value }, statementRequest)
                .map { (amount, description) ->
                    this.addCost(amount, description)
                }
        val transactions = transactionRepository.findAll()
                .map { it.value }
                .filter(statementRequest.filter)
        val statementLines = StatementLines.parse(StatementLine.initial(), transactions)

        return Statement.inReverseOrder(statementLines)
    }

    private fun addCost(cost: Amount, description: String) {
        this.transactionRepository.save(createIdentityFor(Transaction.Cost(Tx(cost, clock.getTime(), description))))
    }

    fun balance(): Amount {
        val transactions = this.findAll()
        val result = transactions.map { it.value }
                .foldRight(Amount.of("0"), { transaction, acc -> transaction.subtotal(acc) })
        return result
    }

    val find = transactionRepository::findBy

    val findAll = transactionRepository::findAll

    class StatementLines {
        companion object {
            fun parse(initialStatement: StatementLine, transactions: List<Transaction>): List<StatementLine> {
                val initial = mutableListOf(initialStatement)
                val statementLines = transactions.fold(initial,
                        { result, current ->
                            val currentStatement = StatementLine.parse(current, result.last().balance)
                            result.add(currentStatement)
                            result
                        })
                return statementLines.toList()
            }


        }

    }

    enum class AccountType {
        Personal {
            override fun determineStatementCost(map: List<Transaction>, statementRequest: AccountRequest.StatementRequest): Option<Pair<Amount, String>> {
                if (map.filter(statementRequest.filter).isNotEmpty()) {
                    return Option.pure(Pair(Amount.Companion.of("1"), "Statement creation"))
                }
                return Option.empty()
            }
        },
        Premium {
            override fun determineStatementCost(map: List<Transaction>, statementRequest: AccountRequest.StatementRequest): Option<Pair<Amount, String>> {
                return Option.empty()
            }

        };

        abstract fun determineStatementCost(map: List<Transaction>, statementRequest: AccountRequest.StatementRequest): Option<Pair<Amount, String>>
    }

    companion object {
        fun transfer(amount: Amount, description: String, from: Persisted<Account>, to: Persisted<Account>): Workflow {
            val part1 = from.value.tryOutgoing(from.value.genTx(amount, description), from, to)
            val part2 = to.value.tryIncoming(to.value.genTx(amount, description), from, to)
            val steps = listOf(part1, part2).map { chooseValue(it) }
            val finalOperation1 = chooseId(part1)
            val finalOperation2 = chooseId(part2)
            return Workflow.from(steps, listOf(Pair(from.value, finalOperation1), Pair(to.value, finalOperation2)))
        }

        private fun chooseId(part1: Either<Persisted<SecureRequest>, Persisted<Transaction.Transfer>>): Id {
            return when (part1) {
                is Either.Left -> {
                    part1.a.id
                }
                is Either.Right -> {
                    part1.b.id
                }
            }
        }

        private fun chooseValue(value: Either<Persisted<SecureRequest>, Persisted<Transaction.Transfer>>): Either<SecureRequest, Transaction.Transfer> {
            return value.map { it.value }.mapLeft { it.value }
        }
    }

    private fun genTx(amount: Amount, description: String) = Tx(amount, clock.getTime(), description)

    fun tryOutgoing(tx: Tx, from: Persisted<Account>, to: Persisted<Account>): Either<Persisted<Transaction.Transfer.SecureRequest>, Persisted<Transaction.Transfer>> {
        val security = this.outgoingSecurity
        var request: Transaction.Transfer = Transaction.Transfer.Emitted(tx, Completed(from.id, to.id))
        if (security.isDefined()) {
            request = Transaction.Transfer.SecureRequest(tx, security.get().generate(), request)
            val persisted = Persisted.`for`(request, Id.random())
            transactionRepository.save(persisted)
            return Either.left(persisted)
        } else {
            val persisted = Persisted.`for`(InsecureRequest(tx, request), Id.random())
            transactionRepository.save(persisted)
            return Either.right(persisted)
        }
    }

    fun tryIncoming(tx: Tx, from: Persisted<Account>, to: Persisted<Account>): Either<Persisted<Transaction.Transfer.SecureRequest>, Persisted<Transaction.Transfer>> {
        val security = this.incomingSecurity
        var request: Transaction.Transfer = Transaction.Transfer.Received(tx, Completed(from.id, to.id))
        if (security.isDefined()) {
            request = Transaction.Transfer.SecureRequest(tx, security.get().generate(), request)
            val persisted = Persisted.`for`(request, Id.random())
            transactionRepository.save(persisted)
            return Either.left(persisted)
        } else {
            val persisted = Persisted.`for`(InsecureRequest(tx, request), Id.random())
            transactionRepository.save(persisted)
            return Either.right(persisted)
        }
    }

    fun confirm(transactionId: Id) {
        this.transactionRepository
                .findBy(transactionId)
                .map {
                    when (it.value) {
                        is SecureRequest -> {
                            transactionRepository.save(Persisted.`for`(it.value.transfer, Id.random()))
                        }
                        is InsecureRequest -> {
                            transactionRepository.save(Persisted.`for`(it.value.transfer, Id.random()))
                        }
                        else -> {
                            throw IllegalArgumentException()
                        }
                    }

                }
    }
}
