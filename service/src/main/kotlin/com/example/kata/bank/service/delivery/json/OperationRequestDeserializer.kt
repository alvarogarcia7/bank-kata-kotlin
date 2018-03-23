package com.example.kata.bank.service.delivery.json


import com.example.kata.bank.service.infrastructure.operations.AmountDTO
import com.example.kata.bank.service.infrastructure.operations.`in`.AccountDTO
import com.example.kata.bank.service.infrastructure.operations.`in`.OperationRequest
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
                    OperationRequest.DepositRequest(amount = readAmount(node), description = string(node, "description"))
                }
                "transfer" -> {
                    OperationRequest.TransferRequest(amount = readAmount(node), destination = readDestination(node), description = string(node, "description"))
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

    private fun readDestination(node: TreeNode): AccountDTO {
        val destinationNode = node.get("destination")
        return AccountDTO(string(destinationNode, "number"), string(destinationNode, "owner"))
    }

    private fun readAmount(node: TreeNode) = AmountDTO.EUR(string(node.get("amount"), "value"))

    private fun string(node: TreeNode, key: String) = (node.get(key) as TextNode).asText()
}