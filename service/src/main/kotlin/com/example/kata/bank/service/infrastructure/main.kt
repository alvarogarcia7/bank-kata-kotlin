package com.example.kata.bank.service.infrastructure

import spark.kotlin.Http
import spark.kotlin.ignite

fun main(args: Array<String>) {
    BankWebApplication.start(8080)
}

object BankWebApplication {
    private var http: Http = ignite()

    fun start(port: Int) {
        val http = http
                .port(port)
                .threadPool(10)

        http.get("/") {
            hello(request.queryParamOrDefault("name", null))
        }
    }

    fun stop() {
        http.stop()
    }

    private fun hello(name: String?) = if (null == name) "Hello, world!" else "Hello $name!"

}