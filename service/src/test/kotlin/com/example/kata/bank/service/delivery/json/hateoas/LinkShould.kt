package com.example.kata.bank.service.delivery.json.hateoas

import com.example.kata.bank.service.domain.Id
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class LinkShould {
    @Test
    fun `generate a self link with a single resource`() {
        val link = Link.self("accounts" to Id.of("1"))
        Assertions.assertThat(link.href).isEqualTo("/accounts/1")
        Assertions.assertThat(link.method).isEqualTo("GET")
        Assertions.assertThat(link.rel).isEqualTo("self")

        val id = Id.of("1")
        assertThat(Link("/accounts/${id.value}", rel = "self", method = "GET")).isEqualTo(link)
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
}