package com.mackie.streams.producer.config.json.serde

import com.mackie.streams.producer.config.JacksonConfiguration
import org.apache.kafka.common.errors.SerializationException
import org.apache.kafka.common.serialization.Deserializer
import com.fasterxml.jackson.databind.JsonNode

class JacksonSerdeDeserializer : Deserializer<JsonNode> {

    private val jackson = JacksonConfiguration().jackson()

    override fun deserialize(topic: String, data: ByteArray): JsonNode? =
        runCatching {
           jackson.readTree(data)
        }.recoverCatching { ex ->
            throw SerializationException("Error on serialization", ex)
        }.getOrNull()
}
