package com.mackie.streams.producer.sender

import com.fasterxml.jackson.databind.ObjectMapper
import com.mackie.streams.producer.config.KafkaConfiguration.Topics.VEHICLE_MAIN
import io.micrometer.prometheus.PrometheusMeterRegistry
import kotlinx.coroutines.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class StatusEventSender(
    private val kafkaSenderService: KafkaSenderService,
    private val objectMapper: ObjectMapper,
    private val meterRegistry: PrometheusMeterRegistry
) {

    private val counter = meterRegistry.counter("app.events.status", "app", "streams-producer")
    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    suspend fun send(event: StatusEvent) = coroutineScope {
        withContext(Dispatchers.IO) {
            async {
                val kafkaResponse = kafkaSenderService.send(
                    topic = VEHICLE_MAIN.topicName,
                    key = event.vin,
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

    suspend fun sendDriveEvents(vin: String, stateOfCharge: Double, stateOfChargeMin: Double): Boolean {
        var kwhLeft = BATT_CAPACITY_KWH * stateOfCharge
        val kwhMin = BATT_CAPACITY_KWH * stateOfChargeMin
        var eventTimePointer = Instant.now()
        withContext(Dispatchers.IO) {
            while (kwhLeft >= kwhMin) {
                logger.info("Drive: ${kwhLeft / BATT_CAPACITY_KWH}, $eventTimePointer")
                launch {
                    when (kafkaSenderService.send(
                        topic = VEHICLE_MAIN.topicName,
                        key = vin,
                        data = objectMapper.valueToTree(
                            StatusEventKafka(
                                vin = vin,
                                customerId = "cust01",
                                stateOfCharge = kwhLeft / BATT_CAPACITY_KWH,
                                createdAt = eventTimePointer
                            )
                        )
                    )) {
                        is KafkaSenderService.KafkaResponse.Success<*, *> -> {
                            counter.increment()
                            delay(100)
                        }

                        else -> {}
                    }
                }
                eventTimePointer = eventTimePointer.plusSeconds(60)
                kwhLeft -= 0.33
            }
        }
        return true
    }

    suspend fun sendChargeEvents(vin: String, stateOfChargeStart: Double, stateOfChargeMax: Double): Boolean {
        var kwhStart = BATT_CAPACITY_KWH * stateOfChargeStart
        val kwhMax = BATT_CAPACITY_KWH * stateOfChargeMax
        var eventTimePointer = Instant.now()
        withContext(Dispatchers.IO) {
            while (kwhMax >= kwhStart) {
                logger.info("Charge: ${kwhStart / BATT_CAPACITY_KWH}, $eventTimePointer")
                launch {
                    when (kafkaSenderService.send(
                        topic = VEHICLE_MAIN.topicName,
                        key = vin,
                        data = objectMapper.valueToTree(
                            StatusEventKafka(
                                vin = vin,
                                customerId = "cust01",
                                stateOfCharge = kwhStart / BATT_CAPACITY_KWH,
                                createdAt = eventTimePointer
                            )
                        )
                    )) {
                        is KafkaSenderService.KafkaResponse.Success<*, *> -> {
                            counter.increment()
                            delay(100)
                        }

                        else -> {}
                    }

                }
                eventTimePointer = eventTimePointer.plusSeconds(60)
                kwhStart += 3
            }
        }
        return true
    }

    data class StatusEvent(
        val vin: String,
        val customerId: String,
        val stateOfCharge: Double
    )

    data class StatusEventKafka(
        val vin: String,
        val customerId: String,
        val stateOfCharge: Double,
        val createdAt: Instant,
    ) {
        companion object {
            fun of(statusEvent: StatusEvent) = StatusEventKafka(
                vin = statusEvent.vin,
                customerId = statusEvent.customerId,
                stateOfCharge = statusEvent.stateOfCharge,
                createdAt = Instant.now()
            )
        }
    }

    companion object {
        private const val BATT_CAPACITY_KWH = 80
    }
}