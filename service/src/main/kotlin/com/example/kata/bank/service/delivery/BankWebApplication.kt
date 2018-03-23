package com.example.kata.bank.service.delivery

import arrow.core.Either
import arrow.core.Option
import arrow.core.Some
import com.example.kata.bank.service.delivery.`in`.StatementRequestDTO
import com.example.kata.bank.service.delivery.application.SparkAdapter
import com.example.kata.bank.service.delivery.handlers.AccountsHandler
import com.example.kata.bank.service.delivery.handlers.OperationsHandler
import com.example.kata.bank.service.delivery.handlers.UsersHandler
import com.example.kata.bank.service.delivery.json.MyResponse
import com.example.kata.bank.service.domain.AccountRequest
import spark.kotlin.Http

class BankWebApplication(
        private val operationsHandler: OperationsHandler,
        private val accountsHandler: AccountsHandler,
        private val usersHandler: UsersHandler) :
        SparkAdapter() {

    override fun configurePaths(http: Http) {
        //accounts
        http.get("/accounts", function = list(accountsHandler::list))
        http.post("/accounts", function = canFail(accountsHandler::add))
        http.get("/accounts/:accountId", function = mayBeMissing(accountsHandler::detail))
        http.post("/accounts/:accountId", function = canFail(accountsHandler::request))

//        operations
        http.get("/accounts/:accountId/operations/:operationId", function = mayBeMissing(operationsHandler::detail))
        http.get("/accounts/:accountId/operations", function = list(operationsHandler::list))
        http.get("/accounts/:accountId/statements/:statementId", function = canFail(operationsHandler::getStatement))
        http.post("/accounts/:accountId/operations", function = canFail(operationsHandler::add))

        //users
        http.get("/users", function = usersHandler.list)
    }
}


class X {
    companion object {
        fun <T> ok(payload: T): ResponseEntity<T> {
            return ResponseEntity(200, Some(payload))
        }

        fun <T> either(map: Either<List<Exception>, MyResponse<T>>): Either<ResponseEntity<List<String>>, ResponseEntity<MyResponse<T>>> {
            return map.bimap({ X.error(it) }, { X.ok(it) })
        }

        private fun error(it: List<Exception>): ResponseEntity<List<String>> {
            return ResponseEntity(400, Some(it.map { it.message!! }))
        }

        fun <T> badRequest(it: T): ResponseEntity<T> {
            return ResponseEntity(400, Some(it))
        }
    }

    data class ResponseEntity<out T>(val statusCode: Int, val payload: Option<T>)
}

class StatementRequestFactory {
    companion object {
        fun create(request: StatementRequestDTO): AccountRequest {
            return AccountRequest.StatementRequest.all()
        }
    }
}

