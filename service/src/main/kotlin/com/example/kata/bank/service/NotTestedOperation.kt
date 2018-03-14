package com.example.kata.bank.service

class NotTestedOperation : Exception() {
    override val message: String?
        get() = "This feature has not been tested yet"
}