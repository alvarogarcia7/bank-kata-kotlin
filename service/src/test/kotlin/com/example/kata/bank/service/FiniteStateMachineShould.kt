package com.example.kata.bank.service

import org.assertj.core.api.Assertions
import org.junit.Test

class FiniteStateMachineShould {
    @Test
    fun `stay at a stable state`() {

        val stableState = StableState()

        val newState = stableState.run()

        Assertions.assertThat(stableState).isEqualTo(newState)
    }
}

interface State {
    fun run(): State
}

class StableState : State {

    override fun run(): State {
        return this
    }

}
