package com.example.kata.bank.service.delivery

import arrow.core.*
import com.example.kata.bank.service.NotTestedOperation
import com.example.kata.bank.service.delivery.`in`.StatementRequestDTO
import com.example.kata.bank.service.delivery.application.SparkAdapter
import com.example.kata.bank.service.delivery.handlers.Handler
import com.example.kata.bank.service.delivery.json.JSONMapper
import com.example.kata.bank.service.delivery.json.MyResponse
import com.example.kata.bank.service.domain.AccountRequest
import spark.Request
import spark.Response
import spark.kotlin.Http
import spark.kotlin.RouteHandler

class BankWebApplication(private vararg val handlers: Handler) : SparkAdapter() {

    private val adapter = BankWebApplication.Companion
    override fun configurePaths(http: Http) {
        handlers.map {
            it.register(http)
        }
    }

    companion object {
        private val objectMapper = JSONMapper.aNew()


        fun <T : Any> many(kFunction2: (Request, Response) -> X.ResponseEntity<T>): RouteHandler.() -> Any = {
            val result = kFunction2.invoke(request, response)
            response.status(result.statusCode)
            result.payload
                    .orElse { Some("") }
                    .map { objectMapper.writeValueAsString(it) }.get()
        }

        fun <T : Any, S : Any> canFail(fn: (Request, Response) -> Either<X.ResponseEntity<T>, X.ResponseEntity<S>>): RouteHandler.() -> Any = {
            val result = fn.invoke(request, response)
            val payload = when (result) {
                is Either.Left<X.ResponseEntity<T>, X.ResponseEntity<S>> -> {
                    response.status(result.a.statusCode)
                    result.a.payload
                }
                is Either.Right<X.ResponseEntity<T>, X.ResponseEntity<S>> -> {
                    response.status(result.b.statusCode)
                    result.b.payload
                }
            }
            val body = when (payload) {
                is Some<Any> -> serialize(payload.t)
                is None -> ""
            }
            body
        }

        fun <T : Any> mayBeMissing(fn: (Request, Response) -> Option<X.ResponseEntity<T>>): RouteHandler.() -> Any = {
            val result = fn.invoke(request, response)
            when (result) {
                is Some<X.ResponseEntity<T>> -> {
                    response.status(result.t.statusCode)
                    val nP = result.t.payload
                    when (nP) {
                        is Some<T> -> serialize(nP.t)
                        is None -> NotTestedOperation()
                    }
                }
                is None -> {
                    response.status(404)
                    ""
                }
            }
        }


        private fun <T> serialize(it: T): String {
            return objectMapper.writeValueAsString(it)
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

