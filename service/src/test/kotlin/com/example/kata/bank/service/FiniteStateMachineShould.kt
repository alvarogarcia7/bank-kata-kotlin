package com.example.kata.bank.service

import org.assertj.core.api.Assertions
import org.junit.Test

class FiniteStateMachineShould {
    private val emptyCar = Car(listOf())

    @Test
    fun `stay at a stable state`() {
        val stableState = FinalState(emptyCar)

        val newState = stableState.run()

        Assertions.assertThat(stableState).isEqualTo(newState)
        Assertions.assertThat(stableState.payload).isEqualTo(newState.payload)
    }

    @Test
    fun `automatically consume the lambda-transitions`() {
        val stableState = FinalState(emptyCar)
        val state = TransitionState(emptyCar, LambdaTransition({ _: State<Car> -> stableState }))

        val newState = state.run()

        Assertions.assertThat(newState).isEqualTo(stableState)
        Assertions.assertThat(stableState.payload).isEqualTo(newState.payload)
    }
}

data class Car(val parts: List<String>)

class TransitionState<T>(override val payload: T, private val lambdaTransition: LambdaTransition<T>) : State<T> {
    override fun run(): State<T> {
        return lambdaTransition.perform(this)
    }

}

class LambdaTransition<T>(private val function: (State<T>) -> State<T>) {
    fun perform(previous: State<T>): State<T> {
        return function.invoke(previous)
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
