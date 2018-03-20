package com.example.etude.statemachine

sealed class Car {
    companion object {
        fun aNew(): Car {
            return InitialCar(listOf())
        }
    }

    data class InitialCar(private val parts: List<String>) : Car() {
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