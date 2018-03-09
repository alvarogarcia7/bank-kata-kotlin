package com.example.kata.bank.service.delivery.json


import com.example.kata.bank.service.infrastructure.operations.AmountDTO
import com.example.kata.bank.service.infrastructure.operations.OperationRequest
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.TreeNode
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.node.TextNode

class OperationRequestDeserializer @JvmOverloads constructor(vc: Class<*>? = null) : StdDeserializer<OperationRequest>(vc) {

    @Throws(IllegalArgumentException::class)
    override fun deserialize(jp: JsonParser, ctxt: DeserializationContext): OperationRequest {
        try {
            val node = jp.codec.readTree<TreeNode>(jp)
            val nodeType = string(node, "type")
            val result = when (nodeType) {
                "deposit" -> {
                    OperationRequest.DepositRequest(amount = AmountDTO.EUR(string(node.get("amount"), "value")), description = string(node, "description"))
                }
                else -> {
                    throw IllegalArgumentException("type not recognized in: $node")
                }
            }
            return result
        } catch (e: Exception) {
            throw IllegalArgumentException("Could not parse this Operation", e)
        }
    }

    private fun string(node: TreeNode, key: String) = (node.get(key) as TextNode).asText()
}