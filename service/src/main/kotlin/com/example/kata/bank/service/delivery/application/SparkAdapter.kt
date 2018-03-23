package com.example.kata.bank.service.delivery.application

import arrow.core.*
import com.example.kata.bank.service.NotTestedOperation
import com.example.kata.bank.service.delivery.X
import com.example.kata.bank.service.delivery.json.JSONMapper
import spark.Request
import spark.Response
import spark.Service
import spark.kotlin.Http
import spark.kotlin.RouteHandler

abstract class SparkAdapter : ApplicationEngine {
    protected var httpService: Http = Http(Service.ignite())

    override fun start(port: Int): SparkAdapter {
        val http = httpService
                .port(port)

        configurePaths(http)
        return this
    }

    override fun stop() {
        httpService.stop()
    }

    companion object {
        private val objectMapper = JSONMapper.aNew()


        fun <T : Any> list(kFunction2: (Request, Response) -> X.ResponseEntity<T>): RouteHandler.() -> Any = {
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

    protected abstract fun configurePaths(http: Http)
}