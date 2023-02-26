package com.mackie.streams.producer.sender

import com.fasterxml.jackson.databind.ObjectMapper
import com.mackie.streams.producer.config.KafkaConfiguration.Topics.VEHICLE_MAIN
import com.mackie.streams.producer.domain.primitives.Vin
import io.micrometer.prometheus.PrometheusMeterRegistry
import kotlinx.coroutines.*
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.Instant

@Service
class StatusEventSender(
    private val kafkaSenderService: KafkaSenderService,
    private val objectMapper: ObjectMapper,
    private val meterRegistry: PrometheusMeterRegistry
) {

    val counter = meterRegistry.counter("app.events.status", "app", "streams-producer")

    suspend fun send(event: StatusEvent) = coroutineScope {
        withContext(Dispatchers.IO) {
            async {
                val kafkaResponse = kafkaSenderService.send(
                    topic = VEHICLE_MAIN.topicName,
                    key = event.vin.value,
                    data = objectMapper.valueToTree(StatusEventKafka.of(event))
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

    suspend fun sendRandom(event: StatusEvent, repeats: Int, frequency: Duration): Boolean {
        withContext(Dispatchers.IO) {
            repeat(repeats) { rep ->
                launch {
                    when(kafkaSenderService.send(
                        topic = VEHICLE_MAIN.topicName,
                        key = event.vin.value,
                        data = objectMapper.valueToTree(StatusEventKafka.of(event.copy(mileage = event.mileage + rep)))
                    )) {
                        is KafkaSenderService.KafkaResponse.Success<*, *> -> {
                            counter.increment()
                            delay(frequency.toMillis())
                        }
                        else -> {}
                    }
                }
            }
        }
        return true
    }

    data class StatusEvent(
        val vin: Vin,
        val customerId: String,
        val mileage: Int,
    )

    data class StatusEventKafka(
        val vin: Vin,
        val customerId: String,
        val createdAt: Instant,
        val mileage: Int,
    ) {
        companion object {
            fun of(statusEvent: StatusEvent) = StatusEventKafka(
                vin = statusEvent.vin,
                customerId = statusEvent.customerId,
                createdAt = Instant.now(),
                mileage = statusEvent.mileage
            )
        }
    }
}