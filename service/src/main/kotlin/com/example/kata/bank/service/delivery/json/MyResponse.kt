package com.example.kata.bank.service.delivery.json

import com.example.kata.bank.service.delivery.json.hateoas.Link

data class MyResponse<out T>(val response: T, val links: List<Link>) {
    companion object {
        fun <T> noLinks(response: T): MyResponse<T> {
            return MyResponse(response, listOf())
        }

        fun <T> links(response: T, vararg links: Link): MyResponse<T> {
            return MyResponse(response, arrayOf(*links).toList())
        }
    }
}