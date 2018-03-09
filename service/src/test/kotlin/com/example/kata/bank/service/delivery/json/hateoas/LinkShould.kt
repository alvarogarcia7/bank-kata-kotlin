package com.example.kata.bank.service.delivery.json.hateoas

import arrow.core.Either
import com.example.kata.bank.service.domain.Id
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.SoftAssertions
import org.junit.jupiter.api.Test

internal class LinkShould {
    @Test
    fun `generate a self link with a single resource`() {
        val id1 = Id.of("1")
        val linkUsingBuilder = Link.self("accounts" to id1)
        val manualLink = Link("/accounts/${id1.value}", rel = "self", method = "GET")

        assertThat(manualLink).isEqualTo(linkUsingBuilder)
    }

    @Test
    fun `generate a self link with multiple resources`() {
        val id1 = Id.of("1")
        val id2 = Id.of("2")
        val linkUsingBuilder = Link.self("accounts" to id1, "operations" to id2)
        val manualLink = Link("/accounts/${id1.value}/operations/${id2.value}", rel = "self", method = "GET")

        assertThat(manualLink).isEqualTo(linkUsingBuilder)
    }

    @Test
    fun `can't generate a link without resources`() {
        // val linkUsingBuilder = Link.self()
        // this will not compile
    }

    @Test
    fun `parse link with a single resource`() {
        val id1 = Id.of("1")
        val link = Link.self("accounts" to id1)

        assertThat(Link.parse(link.href)).isEqualTo(Either.right(link))
    }

    @Test
    fun `parse link with a single resource - one ending slash`() {
        val id1 = Id.of("1")
        val link = Link.self("accounts" to id1)

        assertThat(Link.parse(link.href + "/")).isEqualTo(Either.right(link))
    }

    @Test
    fun `parse link with a single resource - multiple ending slashes`() {
        val id1 = Id.of("1")
        val link = Link.self("accounts" to id1)

        val softly = SoftAssertions()
        softly.assertThat(Link.parse(link.href + "//")).isEqualTo(Either.right(link))
        softly.assertThat(Link.parse(link.href + "///")).isEqualTo(Either.right(link))
        softly.assertThat(Link.parse(link.href + "////")).isEqualTo(Either.right(link))
        softly.assertThat(Link.parse(link.href + "/////")).isEqualTo(Either.right(link))
        softly.assertAll()
    }

    @Test
    fun `parse a self link with multiple resources`() {
        val id1 = Id.of("1")
        val id2 = Id.of("2")
        val link = Link.self("accounts" to id1, "operations" to id2)

        assertThat(Link.parse(link.href)).isEqualTo(Either.right(link))
    }

    @Test
    fun `can't parse an empty link`() {
        assertExceptionsMatch(Link.parse(""), "no resources")
    }

    @Test
    fun `can't parse a link without resource`() {
        assertExceptionsMatch(Link.parse("/"), "no resource")
    }

    @Test
    fun `can't parse a link without resource or its id`() {
        assertExceptionsMatch(Link.parse("//"), "no resource", "no resource id")
    }

    private fun assertExceptionsMatch(actual: Either<List<Exception>, Link>, vararg messages: String) {
        assertThat(actual.mapLeft { it.map { it.message } }).isEqualTo(Either.left(listOf(* messages)))
    }
}