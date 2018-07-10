package com.example.kata.bank.service.delivery

object AccountsHandlerClient {

    fun createAccount(name: String) = """
        {"name": "$name"}
        """.trimIndent()

    fun deposit(amount: String): String {
        return operation(amount, "deposit")
    }

    private fun operation(amount: String, operation: String): String {
        return """
            {
                "type": "$operation",
                "amount": {
                    "value": "$amount",
                    "currency": "EUR"
                },
                "description": "yet another operation"
            }
        """.trimIndent()
    }
}
