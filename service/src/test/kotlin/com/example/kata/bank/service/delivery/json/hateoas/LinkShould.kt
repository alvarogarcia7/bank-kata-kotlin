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
}