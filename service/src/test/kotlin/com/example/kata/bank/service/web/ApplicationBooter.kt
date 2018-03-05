package com.example.kata.bank.service.web

import com.example.kata.bank.service.infrastructure.ApplicationEngine
import java.net.BindException
import java.util.*

class ApplicationBooter<out T : ApplicationEngine>(private val applicationConfigurer: () -> T) {

    val randomGenerator = Random()
    private fun randomUnprivilegedPort(randomGenerator: Random) = randomGenerator.nextInt(3000) + 57000

    fun `atRandomPort`(): Pair<T, Int> {
        val configuredApplication = applicationConfigurer.invoke()
        while (true) {
            val currentPort = randomUnprivilegedPort(randomGenerator)
            println("Trying to start on port $currentPort...")
            try {
                val application = configuredApplication.start(currentPort) as T
                return Pair(application, currentPort)
            } catch (e: BindException) {
                e.printStackTrace()
                println("Port $currentPort is already in use.")
            }
        }
    }

}