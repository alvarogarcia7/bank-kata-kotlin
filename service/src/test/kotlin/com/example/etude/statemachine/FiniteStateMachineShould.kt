package com.example.etude.statemachine

import com.example.etude.statemachine.library.FinalState
import com.example.etude.statemachine.library.State
import com.example.etude.statemachine.library.TransitionState
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


