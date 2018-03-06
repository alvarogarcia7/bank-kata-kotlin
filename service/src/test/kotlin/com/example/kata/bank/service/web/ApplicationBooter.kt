package com.example.kata.bank.service.web

import com.example.kata.bank.service.infrastructure.application.ApplicationEngine
import java.net.BindException
import java.net.ServerSocket

class ApplicationBooter<out T : ApplicationEngine>(private val applicationConfigurer: () -> T) {

    fun `atRandomPort`(): Pair<T, Int> {
        val configuredApplication = applicationConfigurer.invoke()
        val socket = ServerSocket(0)
        val currentPort = socket.localPort
        println("Trying to start on port $currentPort...")
        try {
            val application = configuredApplication.start(currentPort) as T
            return Pair(application, currentPort)
        } catch (e: BindException) {
            e.printStackTrace()
            println("Port $currentPort is already in use.")
            throw RuntimeException(e)
        }
    }
}