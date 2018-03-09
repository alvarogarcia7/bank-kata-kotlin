package com.example.kata.bank.service.delivery.json.hateoas

import com.example.kata.bank.service.domain.Id

data class Link(val href: String, val rel: String, val method: String) {
    companion object {
        fun self(vararg resources: Pair<String, Id>): Link {
            return Link(buildHref(resources), "self", "GET")
        }

        private fun buildHref(resources: Array<out Pair<String, Id>>): String {
            return "/" + resources.map { "${it.first}/${it.second.value}" }
                    .joinToString("/")
        }
    }

}