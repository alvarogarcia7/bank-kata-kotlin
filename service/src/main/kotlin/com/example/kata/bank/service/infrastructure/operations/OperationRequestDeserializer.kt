package com.example.kata.bank.service.infrastructure.operations


import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.core.TreeNode
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.node.TextNode
import java.io.IOException

class OperationRequestDeserializer @JvmOverloads constructor(vc: Class<*>? = null) : StdDeserializer<OperationRequest>(vc) {

    @Throws(IOException::class, JsonProcessingException::class)
    override fun deserialize(jp: JsonParser, ctxt: DeserializationContext): OperationRequest {
        val node = jp.codec.readTree<TreeNode>(jp)
        val result = when (string(node, "type")) {
            "deposit" -> {
                OperationRequest.DepositRequest(amount = AmountDTO.EUR(string(node.get("amount"), "value")), description = string(node, "description"))
            }
            else -> {
                throw IllegalArgumentException("type not recognized in: " + node.toString())
            }
        }
        return result
    }

    private fun string(node: TreeNode, key: String) = (node.get(key) as TextNode).asText()
}