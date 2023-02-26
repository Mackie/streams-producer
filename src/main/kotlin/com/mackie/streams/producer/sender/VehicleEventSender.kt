package com.mackie.streams.producer.sender

import com.fasterxml.jackson.databind.ObjectMapper
import com.mackie.streams.producer.config.KafkaConfiguration.Topics.VEHICLE_DETAILS
import com.mackie.streams.producer.domain.VehicleDetails
import com.mackie.streams.producer.domain.primitives.Vin
import io.micrometer.prometheus.PrometheusMeterRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class VehicleEventSender(
    private val kafkaSenderService: KafkaSenderService,
    private val objectMapper: ObjectMapper,
    private val meterRegistry: PrometheusMeterRegistry
) {

    val counter = meterRegistry.counter("app.events.vehicle", "app", "streams-producer")

    suspend fun send(event: VehicleEvent) = coroutineScope {
        withContext(Dispatchers.IO) {
            async {
                val kafkaResponse = kafkaSenderService.send(
                    topic = VEHICLE_DETAILS.topicName,
                    key = event.vin.value,
                    data = objectMapper.valueToTree(VehicleEventKafka.of(event))
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

    data class VehicleEvent(
        val vin: Vin,
        val details: VehicleDetails,
    )

    data class VehicleEventKafka(
        val vin: Vin,
        val details: VehicleDetails,
        val updatedAt: Instant
    ) {
        companion object {
            fun of(vehicleEvent: VehicleEvent) = VehicleEventKafka(
                vin = vehicleEvent.vin,
                details = vehicleEvent.details,
                updatedAt = Instant.now()
            )
        }
    }
}