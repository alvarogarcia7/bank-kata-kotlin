package com.example.kata.bank.service

import com.example.kata.bank.service.delivery.application.ApplicationEngine
import java.net.BindException
import java.util.*

class ApplicationBooter<out T : ApplicationEngine>(private val applicationConfigurer: () -> T) {

    fun `atRandomPort`(): Pair<T, Int> {
        val configuredApplication = applicationConfigurer.invoke()
        while (true) {
//            do not use like this:
//            val socket = ServerSocket(0)
//            val currentPort = socket.localPort
            val currentPort = Random().nextInt(3000) + 56000
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
}