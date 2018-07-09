package com.example.kata.bank.service

interface State<T> {
    fun transition(): State<T>
}

open class FinalState<T> : State<T> {
    override fun transition(): State<T> {
        return this
    }
}