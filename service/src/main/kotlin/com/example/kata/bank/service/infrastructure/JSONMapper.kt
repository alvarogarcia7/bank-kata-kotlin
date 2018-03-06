package com.example.kata.bank.service.infrastructure

import com.example.kata.bank.service.infrastructure.operations.OperationRequestDeserializer
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper

class JSONMapper {
    companion object {
        fun aNew(): ObjectMapper {
            val mapper = jacksonObjectMapper()
            mapper.enable(SerializationFeature.INDENT_OUTPUT)
            val module = SimpleModule()
            module.addDeserializer(BankWebApplication.OperationRequest::class.java, OperationRequestDeserializer())
            mapper.registerModule(module)
            return mapper
        }
    }
}
