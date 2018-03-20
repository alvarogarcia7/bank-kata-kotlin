package com.example.kata.bank.service

import org.assertj.core.api.Assertions
import org.junit.Test

class FiniteStateMachineShould {
    private val emptyCar = Car.aNew()

    @Test
    fun `stay at a stable state`() {
        val stableState = FinalState(emptyCar)

        val newState = stableState.run()

        Assertions.assertThat(stableState).isEqualTo(newState)
        Assertions.assertThat(stableState.payload).isEqualTo(newState.payload)
    }

    @Test
    fun `automatically consume the lambda-transitions, as they are defined in the set of transtions`() {
        val stableState = FinalState(emptyCar)
        val state = TransitionState(emptyCar, { _: State<Car> -> stableState })

        val newState = state.run()

        Assertions.assertThat(newState).isEqualTo(stableState)
        Assertions.assertThat(stableState.payload).isEqualTo(newState.payload)
    }

    @Test
    fun `configure the state with multiple transitions`() {

        val transitions = { state: State<Car> ->
            when (state.payload) {
                is Car.FinishedCar -> {
                    println("the car has been finished")
                    FinalState(state.payload)
                }
                else -> {
                    FinalState(state.payload.putWheels())
                }
            }
        }
        val state = TransitionState(emptyCar, transitions)

        val newState = state.run()

        Assertions.assertThat(newState.payload.javaClass.simpleName).isEqualTo("FinishedCar")
        Assertions.assertThat(newState.payload.hasWheels()).isTrue()
    }
}

sealed class Car(private val parts: List<String> = listOf()) {
    companion object {
        fun aNew(): Car {
            return InitialCar(listOf())
        }
    }

    fun hasWheels(): Boolean {
        return parts.contains("wheels")
    }

    fun putWheels(): Car {
        val parts = this.parts.toMutableList()
        parts.add("wheels")
        return FinishedCar(parts.toList())
    }

    data class FinishedCar(val parts: List<String>) : Car(parts)
    data class InitialCar(val parts: List<String>) : Car(parts)

}


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
