package com.example.etude.statemachine.library

class TransitionState<T>(override val payload: T, private val transitions: (State<T>) -> State<T>) : State<T> {
    override fun transition(): State<T> {
        return transitions.invoke(this)
    }
}

interface State<T> {
    fun transition(): State<T>
    val payload: T
}

class FinalState<T>(override val payload: T) : State<T> {
    override fun transition(): State<T> {
        return this
    }
}