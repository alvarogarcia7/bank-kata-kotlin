package com.example.kata.bank.service

import com.example.kata.bank.service.delivery.json.JSONMapper
import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.fuel.core.Request
import com.github.kittinunf.fuel.core.Response
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.fuel.httpPost
import com.github.kittinunf.result.Result
import org.assertj.core.api.Assertions

object HTTP {

    val mapper = JSONMapper.aNew()

    private fun serialize(body: Any): String {
        return mapper.writeValueAsString(body)
    }

    fun post(url: String, body: Any): Request {
        val serializedBody = when (body) {
            is String -> body
            else -> serialize(body)
        }
        return url.httpPost().header("Content-Type" to "application/json").body(serializedBody, Charsets.UTF_8)
    }

    fun get(url: String): Request {
        return url.httpGet()
    }

    fun request(request: Request): Pair<Response, Result.Success<String, FuelError>> {
        try {
            val (_, response, result) = request.responseString()
            return assertSuccess(response, result)
        } catch (e: Exception) {
            e.printStackTrace()
            Assertions.fail("exception: " + e.message)
            throw RuntimeException() // unreachable code
        }
    }

    fun <T> assertFailedRequest(
            request: Request,
            x: (Response, Result<String, FuelError>) -> T): T {
        try {
            val (_, response, result) = request.responseString()
            return x.invoke(response, result)
        } catch (e: Exception) {
            e.printStackTrace()
            Assertions.fail("exception: " + e.message)
            throw RuntimeException() // unreachable code
        }
    }

    @Throws(UnreachableCode::class)
    fun assertSuccess(response: Response, result: Result<String, FuelError>): Pair<Response, Result.Success<String, FuelError>> {
        return when (result) {
            is Result.Success -> {
                val pair = Pair(response, result)
                pair
            }
            is Result.Failure -> {
                Assertions.fail("expected a Result.success: " + result.error)
                throw UnreachableCode()
            }
            else -> {
                Assertions.fail("expected a Result.success: " + result.javaClass)
                throw UnreachableCode()
            }
        }
    }

    @Throws(UnreachableCode::class)
    fun assertError(response: Response, result: Result<String, FuelError>): Pair<Response, Result.Failure<String, FuelError>> {
        return when (result) {
            is Result.Success -> {
                Assertions.fail("expected a Result.error: " + result.value)
                throw UnreachableCode()
            }
            is Result.Failure -> {
                Pair(response, result)
            }
            else -> {
                Assertions.fail("expected a Result.error: " + result.javaClass)
                throw UnreachableCode()
            }
        }
    }

}