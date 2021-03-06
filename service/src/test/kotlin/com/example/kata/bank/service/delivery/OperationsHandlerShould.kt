package com.example.kata.bank.service.delivery

import arrow.core.Either
import com.example.kata.bank.service.delivery.handlers.OperationsHandler
import com.example.kata.bank.service.delivery.json.JSONMapper
import com.example.kata.bank.service.delivery.json.MyResponse
import com.example.kata.bank.service.delivery.out.ErrorsDTO
import com.example.kata.bank.service.infrastructure.accounts.AccountRestrictedRepository
import com.example.kata.bank.service.infrastructure.operations.AmountDTO
import com.example.kata.bank.service.infrastructure.operations.OperationService
import com.example.kata.bank.service.infrastructure.operations.OperationsRepository
import com.example.kata.bank.service.infrastructure.operations.`in`.OperationRequest
import com.example.kata.bank.service.usecases.accounts.DepositUseCase
import com.example.kata.bank.service.usecases.accounts.TransferUseCase
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import spark.Request
import spark.Response

internal class OperationsHandlerShould {
    private val operationService = OperationService()
    private val accountRepository = AccountRestrictedRepository.aNew()
    private val operationsHandler = OperationsHandler(accountRepository, TransferUseCase(accountRepository), DepositUseCase(accountRepository), OperationsRepository())
    private val fakeResponse: Response = Mockito.mock(Response::class.java)
    @Test
    fun `complain when you don't have an account id`() {
        val fakeRequest = mock<Request> {
            on { it.body() } doReturn serialize(OperationRequest.DepositRequest(AmountDTO.EUR("21.00"), "deposit 1"))
        }

        val result = operationsHandler.add(fakeRequest, fakeResponse)

        assertThat(result).isEqualTo(Either.left(X.badRequest(MyResponse.noLinks(ErrorsDTO.from(listOf(Exception("Needs an :accountId")))))))
    }

    private fun serialize(depositRequest: OperationRequest.DepositRequest): String {
        return JSONMapper.aNew().writeValueAsString(depositRequest)
    }
}