package com.example.kata.bank.service.delivery

import arrow.core.Either
import arrow.core.Option
import arrow.core.Some
import com.example.kata.bank.service.delivery.`in`.StatementRequestDTO
import com.example.kata.bank.service.delivery.application.SparkAdapter
import com.example.kata.bank.service.delivery.handlers.Handler
import com.example.kata.bank.service.delivery.json.MyResponse
import com.example.kata.bank.service.domain.AccountRequest
import spark.kotlin.Http

class BankWebApplication(private vararg val handlers: Handler) : SparkAdapter() {
    override fun configurePaths(http: Http) {
        handlers.map {
            it.register(http)
        }
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

