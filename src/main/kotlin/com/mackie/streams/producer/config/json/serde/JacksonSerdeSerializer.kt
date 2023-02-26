package com.mackie.streams.producer.config.json.serde

import com.mackie.streams.producer.config.JacksonConfiguration
import org.apache.kafka.common.errors.SerializationException
import org.apache.kafka.common.serialization.Serializer

class JacksonSerdeSerializer<T> : Serializer<T> {

    private val jackson = JacksonConfiguration().jackson()

    override fun serialize(topic: String?, data: T): ByteArray? =
        runCatching {
            jackson.writeValueAsBytes(data)
        }.recoverCatching { ex ->
            throw SerializationException("Error on serialization", ex)
        }.getOrNull()
}
