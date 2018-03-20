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

        val state = TransitionState(emptyCar, { state: State<Car> ->
            when (state.payload) {
                is Car.InitialCar -> {
                    val car = (state.payload as Car.InitialCar).putWheels()
                    FinalState(car)
                }
                else -> {
                    FinalState(state.payload)
                }
            }
        })

        val newState = state.run()

        Assertions.assertThat((newState.payload.javaClass.simpleName)).isEqualTo(Car.Assembled::class.java.simpleName)
        //If it is assembled, it means that it has wheels already
    }


    @Test
    fun `configure the multiple state transitions`() {

        val state = TransitionState(emptyCar, { state: State<Car> ->
            when (state.payload) {
                is Car.InitialCar -> {
                    val car = (state.payload as Car.InitialCar).putWheels()
                    FinalState(car)
                }
                else -> {
                    FinalState(state.payload)
                }
            }
        })

        val newState = state.run()

        val state2 = TransitionState(newState.payload, { state: State<Car> ->
            when (state.payload) {
                is Car.Assembled -> {
                    FinalState((state.payload as Car.Assembled).paint("blue"))
                }
                else -> {
                    FinalState(state.payload)
                }
            }
        })

        val newNewState = state2.run()


        Assertions.assertThat((newState.payload.javaClass.simpleName)).isEqualTo(Car.Assembled::class.java.simpleName)
        Assertions.assertThat((newNewState.payload as Car.FinishedCar).color).isEqualTo("blue")
    }
}

sealed class Car {
    companion object {
        fun aNew(): Car {
            return InitialCar(listOf())
        }
    }


    data class InitialCar(private val parts: List<String>) : Car() {
        fun hasWheels(): Boolean {
            return parts.contains("wheels")
        }

        fun putWheels(): Car {
            val parts = this.parts.toMutableList()
            parts.add("wheels")
            val base = InitialCar(parts)
            return Assembled(base)
        }
    }

    data class Assembled(val car: InitialCar) : Car() {
        fun paint(color: String): FinishedCar {
            return FinishedCar(this, color)
        }
    }

    data class FinishedCar(val car: Assembled, val color: String) : Car()
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
