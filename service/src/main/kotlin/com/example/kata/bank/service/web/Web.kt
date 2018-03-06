package com.example.kata.bank.service.web

data class Link(val href: String, val rel: String, val method: String) {

}

data class MyResponse<out T>(val payload: T, val links: List<Link>)