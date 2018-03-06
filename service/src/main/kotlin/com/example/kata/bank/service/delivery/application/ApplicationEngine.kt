package com.example.kata.bank.service.delivery.application

interface ApplicationEngine {
    fun start(port: Int): ApplicationEngine
    fun stop()
}