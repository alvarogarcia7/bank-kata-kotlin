package com.example.kata.bank.service.delivery.handlers

import com.example.kata.bank.service.delivery.json.JSONMapper
import com.example.kata.bank.service.delivery.json.MyResponse
import com.example.kata.bank.service.delivery.json.hateoas.Link
import com.example.kata.bank.service.infrastructure.users.UsersSimpleRepository
import spark.kotlin.Http
import spark.kotlin.RouteHandler

class UsersHandler(private val usersRepository: UsersSimpleRepository) : Handler {
    override fun register(http: Http) {
        http.get("/users", function = list)
    }

    private val objectMapper = JSONMapper.aNew()
    val list: RouteHandler.() -> String = {
        val result = usersRepository
                .findAll()
                .map { (user, id) -> MyResponse.links(user, Link.self(Pair("users", id))) }
        objectMapper.writeValueAsString(result)
    }

}