package com.example.etude.statemachine

import com.example.etude.statemachine.library.FinalState
import com.example.etude.statemachine.library.State


sealed class Car : com.example.etude.statemachine.library.State<Car> {
    companion object {
        fun aNew(): Car {
            return InitialCar(listOf())
        }
    }

    override val payload: Car
        get() = this

    data class InitialCar(private val parts: List<String>) : Car() {

        override fun transition(): State<Car> {
            return when (payload) {
                is Car.InitialCar -> {
                    val car = this.putWheels()
                    FinalState(car)
                }
                else -> {
                    FinalState(payload)
                }
            }
        }

        fun putWheels(): Car {
            val parts = this.parts.toMutableList()
            parts.add("wheels")
            val base = InitialCar(parts)
            return Assembled(base)
        }
    }

    data class Assembled(val car: InitialCar) : Car() {
        override fun transition(): State<Car> {
            return FinalState(paint("blue"))
        }

        fun paint(color: String): Car {
            return Painted(this, color)
        }

    }

    data class Painted(val car: Assembled, val color: String) : Car() {
        override fun transition(): State<Car> {
            return FinalState(finished()).transition()
        }

        private fun finished(): Car {
            return Finished(this)
        }

    }

    data class Finished(val car: Painted) : Car() {
        override fun transition(): State<Car> {
            return FinalState(this as Car).transition()
        }
    }
}