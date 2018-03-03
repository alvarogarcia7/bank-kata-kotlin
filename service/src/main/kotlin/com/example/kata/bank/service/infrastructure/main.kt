package com.example.kata.bank.service.infrastructure

import io.ktor.application.call
import io.ktor.http.ContentType
import io.ktor.response.respondText
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty

fun main(args: Array<String>) {
    Application.server(8080).start(wait = true)
}

object Application {
    fun server(port: Int): ApplicationEngine {
        val server = embeddedServer(Netty, port) {
            routing {
                get("/") {
                    val name = call.parameters["name"]
                    call.respondText(hello(name), ContentType.Text.Html)
                }
            }
        }
        return server
    }

    private fun hello(name: String?) = if (null == name) "Hello, world!" else "Hello $name!"

}