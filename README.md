# Bank kata

I've practiced the [bank kata](https://github.com/sandromancuso/Bank-kata) by [Sandro Mancuso](https://twitter.com/sandromancuso)

My goals where to practice a mix of outside-in + classicist mix of TDD (an idea by [Manuel Rivero](https://twitter.com/trikitrok)) 

Keeping track of the time using git commits.

## Feedback

  - The ability to refactor without breaking the tests is quite good, satisfying even
  - Testing at the almost-boundaries does not cover all the cases, but it's a step in the good direction

## Development notes

### Safe transfers

I've implemented the feature of safe transfers: the account can be
configured to request a code (e.g., OTP) to verify the wire transfer,
both outgoing (e.g., most banks have this) as incoming.

As parameters of the Account class: [Code](https://github.com/alvarogarcia7/bank-kata-kotlin/tree/variant/control-safe-transfers-as-parameters)

As a state machine: [Code](https://github.com/alvarogarcia7/bank-kata-kotlin/tree/variant/control-safe-transfers-as-state-machine)

As Either (i.e., failed computation): [Code](https://github.com/alvarogarcia7/bank-kata-kotlin/tree/variant/control-safe-transfers-as-either)

As thunks (i.e., delayed computations): [Code](https://github.com/alvarogarcia7/bank-kata-kotlin/tree/variant/control-safe-transfers-as-thunks)
