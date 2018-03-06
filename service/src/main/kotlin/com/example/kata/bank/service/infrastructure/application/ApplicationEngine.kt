package com.example.kata.bank.service.infrastructure.application

interface ApplicationEngine {
    fun start(port: Int): ApplicationEngine
    fun stop()
}