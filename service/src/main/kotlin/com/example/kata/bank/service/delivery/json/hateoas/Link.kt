package com.example.kata.bank.service.delivery.json.hateoas

import arrow.core.Either
import arrow.core.Option
import com.example.kata.bank.service.domain.Id

data class Link(val href: String, val rel: String, val method: String) {
    fun resource(resourceName: String): Option<Pair<String, String>> {
        return inPairs(href)
                .map { it.first { it.first == resourceName } }
                .toOption()
    }

    companion object {
        fun self(first: Pair<String, Id>, vararg resources: Pair<String, Id>): Link {
            return Link(buildHref(arrayOf(first, *resources)), "self", "GET")
        }

        private fun buildHref(resources: Array<out Pair<String, Id>>): String {
            return "/" + resources.map { "${it.first}/${it.second.value}" }
                    .joinToString("/")
        }

        fun parse(value: String): Either<List<Exception>, Link> {


            val pairs = inPairs(value)
                    .map { it -> it.map { (resource, idValue) -> Pair(resource, Id.of(idValue)) } }
                    .map { pairs -> Link.self(pairs.first(), *pairs.subList(1, pairs.size).toTypedArray()) }
            return pairs
        }

        fun inPairs(it: String): Either<List<Exception>, List<Pair<String, String>>> {
            val values = it.split("/")
            if (values.size == 1) { // a single chunk
                return Either.left(listOf(Exception("no resources")))
            }
            var i = 1 // need to skip the first slash
            val result: MutableList<Pair<String, String>> = mutableListOf()
            val exceptions = mutableListOf<Exception>()
            while (i < values.size - 1) {
                val resource = values[i]
                if (resource.isNullOrBlank()) {
                    exceptions.add(Exception("no resource"))
                }
                if (i + 1 < values.size) {
                    val id = values[i + 1]
                    i += 2
                    if (id.isNullOrBlank()) {
                        exceptions.add(Exception("no resource id"))
                        break
                    }
                    result.add(Pair(resource, id))
                } else {
                    break
                }
            }
            if (i == 1) {
                exceptions.add(Exception("no resource"))
            }
            return if (result.isEmpty()) {
                Either.left(exceptions)
            } else {
                Either.right(result.toList())
            }
        }
    }

}