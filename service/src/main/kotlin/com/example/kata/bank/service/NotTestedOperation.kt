package com.example.kata.bank.service

class NotTestedOperation : Throwable() {
    override val message: String?
        get() = "This feature has not been tested yet"
}