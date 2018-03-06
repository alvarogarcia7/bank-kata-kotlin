package com.example.kata.bank.service.infrastructure

data class HelloRequest(val name: String?)
open class HelloService {
    open fun salute(request: HelloRequest): String {
        fun hello(name: String?) = if (null == name) "Hello, world!" else "Hello $name!"
        return hello(request.name)
    }

}