package com.mackie.streams.producer.sender

import com.fasterxml.jackson.databind.ObjectMapper
import com.mackie.streams.producer.config.KafkaConfiguration.Topics.CUSTOMER
import io.micrometer.prometheus.PrometheusMeterRegistry
import kotlinx.coroutines.*
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class CustomerEventSender(
    private val kafkaSenderService: KafkaSenderService,
    private val objectMapper: ObjectMapper,
    private val meterRegistry: PrometheusMeterRegistry
) {

    val counter = meterRegistry.counter("app.events.customer", "app", "streams-producer")

    suspend fun send(event: CustomerEvent) = coroutineScope {
        withContext(Dispatchers.IO) {
            async {
                val kafkaResponse = kafkaSenderService.send(
                    topic = CUSTOMER.topicName,
                    key = event.customerId,
                    data = objectMapper.valueToTree(CustomerEventKafka.of(event))
                )

                when (kafkaResponse) {
                    is KafkaSenderService.KafkaResponse.Success<*, *> -> {
                        counter.increment()
                        true
                    }
                    else -> false
                }
            }.await()
        }
    }

    data class CustomerEvent(
        val customerId: String,
        val profession: String,
        val age: Int
    )

    data class CustomerEventKafka(
        val customerId: String,
        val profession: String,
        val age: Int,
        val updatedAt: Instant,
    ) {
        companion object {
            fun of(customerEvent: CustomerEvent) = CustomerEventKafka(
                customerId = customerEvent.customerId,
                profession = customerEvent.profession,
                age = customerEvent.age,
                updatedAt = Instant.now()
            )
        }
    }
}