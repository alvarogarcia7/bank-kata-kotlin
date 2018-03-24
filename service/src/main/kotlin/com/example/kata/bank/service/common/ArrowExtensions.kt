package com.example.kata.bank.service.common

import arrow.core.Either
import arrow.core.Option

inline fun <LEFT, RIGHT> Option<RIGHT>.toEither(function: () -> LEFT): Either<LEFT, RIGHT> {
    return this.fold({ Either.left(function.invoke()) }, { Either.right(this.get()) })
}