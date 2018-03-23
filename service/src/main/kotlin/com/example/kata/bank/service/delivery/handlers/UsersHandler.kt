package com.example.kata.bank.service.delivery.handlers

import com.example.kata.bank.service.delivery.json.JSONMapper
import com.example.kata.bank.service.delivery.json.MyResponse
import com.example.kata.bank.service.delivery.json.hateoas.Link
import com.example.kata.bank.service.infrastructure.users.UsersSimpleRepository
import spark.kotlin.RouteHandler

class UsersHandler(private val usersRepository: UsersSimpleRepository) {
    private val objectMapper = JSONMapper.aNew()
    val list: RouteHandler.() -> String = {
        val result = usersRepository
                .findAll()
                .map { (user, id) -> MyResponse.links(user, Link.self(Pair("users", id))) }
        objectMapper.writeValueAsString(result)
    }

}