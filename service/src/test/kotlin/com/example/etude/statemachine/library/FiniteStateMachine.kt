package com.example.etude.statemachine.library

class TransitionState<T>(override val payload: T, private val transitions: (State<T>) -> State<T>) : State<T> {
    override fun run(): State<T> {
        return transitions.invoke(this)
    }
}

interface State<T> {
    fun run(): State<T>
    val payload: T
}

class FinalState<T>(override val payload: T) : State<T> {
    override fun run(): State<T> {
        return this
    }
}