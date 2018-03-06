package com.example.kata.bank.service.delivery

data class Link(val href: String, val rel: String, val method: String) {

}

data class MyResponse<out T>(val payload: T, val links: List<Link>)