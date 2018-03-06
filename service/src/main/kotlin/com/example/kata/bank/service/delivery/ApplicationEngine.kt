package com.example.kata.bank.service.delivery

interface ApplicationEngine {
    fun start(port: Int): ApplicationEngine
    fun stop()
}