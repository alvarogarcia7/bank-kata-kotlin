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

    @Test
    fun `automatically consume the lambda-transitions`() {

        val stableState = StableState()
        val state = NonStableState(LambdaTransition({ _: State -> stableState }))

        val newState = state.run()

        Assertions.assertThat(newState).isEqualTo(stableState)
    }
}

class NonStableState(private val lambdaTransition: LambdaTransition) : State {
    override fun run(): State {
        return lambdaTransition.perform(this)
    }

}

class LambdaTransition(private val function: (State) -> StableState) {
    fun perform(previous: State): State {
        return function.invoke(previous)
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
