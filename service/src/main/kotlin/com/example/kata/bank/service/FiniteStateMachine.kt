package com.example.kata.bank.service

class TransitionState<T>(val payload: T, private val transitions: (State<T>) -> State<T>) : State<T> {
    override fun transition(): State<T> {
        return transitions.invoke(this)
    }
}

interface State<T> {
    fun transition(): State<T>
}

open class FinalState<T> : State<T> {
    override fun transition(): State<T> {
        return this
    }
}