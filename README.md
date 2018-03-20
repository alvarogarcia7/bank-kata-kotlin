# Bank kata

I've practiced the [bank kata](https://github.com/sandromancuso/Bank-kata) by [Sandro Mancuso](https://twitter.com/sandromancuso)

My goals where to practice a mix of outside-in + classicist mix of TDD (an idea by [Manuel Rivero](https://twitter.com/trikitrok)) 

Keeping track of the time using git commits.

## Feedback

  - The ability to refactor without breaking the tests is quite good, satisfying even
  - Testing at the almost-boundaries does not cover all the cases, but it's a step in the good direction

## Development notes

### Variants for Safe transfers

I've implemented the feature of safe transfers: the account can be
configured to request a code (e.g., OTP) to verify the wire transfer,
both outgoing (e.g., most banks have this) as incoming.

#### Parameters

As parameters of the Account class: [Code](https://github.com/alvarogarcia7/bank-kata-kotlin/tree/variant/control-safe-transfers-as-parameters)

This is the simplest approach: depend on the type of the parameter to
decide the behaviour of the class.

#### State machine

As a state machine: [Code](https://github.com/alvarogarcia7/bank-kata-kotlin/tree/variant/control-safe-transfers-as-state-machine)

Configure the states and the transitions as an internal/external part of
the Transfer class, therefore making it more generic and future-proof (YAGNI?)

This can be represented using inheritance from a common class (in this
case, Transfer) or using a wrapper (State<>) to signify the current state.
For the former, there's the production code. For the latter, a side étude
implementing a state machine library (using a Car + its factory as the domain)

#### Either

As Either (i.e., failed computation): [Code](https://github.com/alvarogarcia7/bank-kata-kotlin/tree/variant/control-safe-transfers-as-either)

Either allows you to represent two explicit computation results. Left has
been used to mean blocked/safe transfer and Right to mean unblocked transfer.

#### Thunks

As thunks (i.e., delayed computations): [Code](https://github.com/alvarogarcia7/bank-kata-kotlin/tree/variant/control-safe-transfers-as-thunks)

A thunk has been passed as a parameter and executed when it is necessary.
This system does not allow for easy persistance/storage, as functions can't
be serialized/deserialized.
