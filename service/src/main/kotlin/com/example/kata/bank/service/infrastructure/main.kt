package com.example.kata.bank.service.infrastructure

import spark.kotlin.Http
import spark.kotlin.RouteHandler
import spark.kotlin.ignite

fun main(args: Array<String>) {
    BankWebApplication(HelloService()).start(8080)
}

class BankWebApplication(private val helloService: HelloService) : ApplicationEngine {
    private var http: Http = ignite()
    private val helloHandler: RouteHandler.() -> String = {
        HelloRequest(request.queryParamOrDefault("name", null))
                .let {
                    helloService.salute(it)
                }
    }

    override fun start(port: Int): BankWebApplication {
        val http = http
                .port(port)
                .threadPool(10)

        http.get("/", function = helloHandler)
        return this
    }

    override fun stop() {
        http.stop()
    }


}

interface ApplicationEngine {
    fun start(port: Int): ApplicationEngine
    fun stop()
}

data class HelloRequest(val name: String?)

class HelloService {
    fun salute(request: HelloRequest): String {
        fun hello(name: String?) = if (null == name) "Hello, world!" else "Hello $name!"
        return hello(request.name)
    }

}
