package com.example.kata.bank.service.delivery.handlers

import spark.kotlin.Http

interface Handler {
    fun register(http: Http)
}
