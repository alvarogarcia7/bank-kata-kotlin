package com.example.kata.bank.service.delivery.json

import arrow.core.Either
import com.example.kata.bank.service.infrastructure.operations.OperationRequest
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue

class JSONMapper {
    companion object {
        fun aNew(): ObjectMapper {
            val mapper = jacksonObjectMapper()
            mapper.enable(SerializationFeature.INDENT_OUTPUT)
            val module = SimpleModule()
            module.addDeserializer(OperationRequest::class.java, OperationRequestDeserializer())
            mapper.registerModule(module)
            return mapper
        }
    }
}


inline fun <reified T : Any> ObjectMapper.readValueOption(body: String): Either<Exception, T> {
    return try {
        Either.right(this.readValue(body))
    } catch (e: Exception) {
        Either.left(e)
    }
}