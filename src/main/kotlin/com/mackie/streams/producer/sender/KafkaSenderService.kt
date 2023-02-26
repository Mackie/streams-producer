package com.mackie.streams.producer.sender

import com.fasterxml.jackson.databind.JsonNode
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@Service
class KafkaSenderService(
    private val kafkaTemplate: KafkaTemplate<String, JsonNode>,
) {

    suspend fun send(topic: String, key: String, data: JsonNode): KafkaResponse {
        return kafkaTemplate.suspendSend(topic, key, data)
    }

    private suspend fun <K : Any, V : Any> KafkaTemplate<K, V>.suspendSend(
        topic: String,
        key: K,
        data: V
    ): KafkaResponse {
        return suspendCoroutine { continuation ->
            this.send(topic, key, data).handleAsync { t, u ->
                if (t != null) {
                    continuation.resume(KafkaResponse.Success(t.recordMetadata, t.producerRecord))
                } else if (u == null) {
                    continuation.resume(KafkaResponse.Empty(topic, key.toString()))
                } else {
                    continuation.resume(KafkaResponse.Failure(u))
                }
            }
        }
    }

    sealed class KafkaResponse {
        data class Success<K, V>(
            val recordMetadata: RecordMetadata,
            val producerRecord: ProducerRecord<K, V>
        ) : KafkaResponse()

        data class Empty(
            val topic: String,
            val key: String
        ) : KafkaResponse()

        data class Failure(
            val throwable: Throwable
        ) : KafkaResponse()
    }
}