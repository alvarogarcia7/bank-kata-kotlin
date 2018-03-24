package com.example.kata.bank.service.infrastructure.operations.`in`

import arrow.core.Either
import com.example.kata.bank.service.domain.Id
import com.example.kata.bank.service.domain.accounts.Account
import com.example.kata.bank.service.domain.transactions.Amount
import com.example.kata.bank.service.infrastructure.operations.AmountDTO
import com.example.kata.bank.service.usecases.accounts.DepositUseCase
import com.example.kata.bank.service.usecases.accounts.TransferUseCase

sealed class OperationRequest {
    data class DepositRequest(val amount: AmountDTO, val description: String) : OperationRequest() {
        fun toUseCase(): DepositUseCase.Request {
            return DepositUseCase.Request(Amount.of(this.amount.value), this.description)
        }
    }

    data class TransferRequest(val amount: AmountDTO, val destination: AccountDTO, val description: String) : OperationRequest() {
        fun toUseCase(): TransferUseCase.In {
            val operationRequest = this
            val amount = Amount.of(operationRequest.amount.value)
            val description = operationRequest.description
            return TransferUseCase.In(Account.Number.of(operationRequest.destination.number), amount, description)
        }
    }

    fun getAssociatedCommand1(): DepositCommand1 {
        TODO()
    }

    fun getAssociatedCommand2(of: Id): DepositCommand2 {
        TODO()
    }

    fun getAssociatedCommand3(of: Id, operationRequest: OperationRequest): DepositCommand3 {
        TODO()
    }
}

class DepositCommand1 {
    fun perform(of: Id, operationRequest: OperationRequest): DepositCommand1 {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    fun get(): Either<List<Exception>, Id> {
        TODO("")
    }
}

class DepositCommand2 {
    fun perform(operationRequest: OperationRequest): DepositCommand2 {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    fun get(): Either<List<Exception>, Id> {
        TODO("")
    }
}

class DepositCommand3 {
    fun perform(): DepositCommand3 {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    fun get(): Either<List<Exception>, Id> {
        TODO("")
    }
}

interface Command {
    fun perform()
}

data class AccountDTO(val number: String, val owner: String)
