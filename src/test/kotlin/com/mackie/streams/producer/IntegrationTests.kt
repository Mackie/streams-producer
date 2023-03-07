package com.mackie.streams.producer

import com.fasterxml.jackson.databind.JsonNode
import com.mackie.streams.producer.sender.KafkaSenderService
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.mockito.kotlin.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.test.mock.mockito.SpyBean
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.stereotype.Component
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.reactive.function.BodyInserters
import java.time.Duration
import java.time.Instant

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(IntegrationTests.Support::class)
@EmbeddedKafka(partitions = 3, brokerProperties = ["listeners=PLAINTEXT://localhost:9092", "port=9092"])
class IntegrationTests {

    private val timeout = Duration.ofSeconds(500).toMillis()

    @Autowired
    private lateinit var webClient: WebTestClient

    @SpyBean
    private lateinit var consumer: Support.KafkaTestConsumer

    @SpyBean
    private lateinit var kafkaSenderService: KafkaSenderService

    @AfterEach
    fun afterEach() {
        reset(consumer)
        reset(kafkaSenderService)
    }

    @Test
    fun `a status event sent is produced and consumed properly in kafka`() {
        val payload = """
                {
                  "vin": "1",
                  "customerId": "1x1",
                  "stateOfCharge": 1.0
                }
            """.trimIndent()

        val argumentCaptor = argumentCaptor<ConsumerRecord<String, JsonNode>>()
        webClient.post()
            .uri("/status")
            .header("auth", "boomer")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .body(
                BodyInserters.fromValue(payload)
            )
            .exchange()
            .expectStatus().isOk

        verify(consumer, timeout(timeout)).receive(argumentCaptor.capture())
        val consumedNode = argumentCaptor.firstValue.value()
        assertThat(consumedNode.get("vin").textValue()).isEqualTo("1")
        assertThat(consumedNode.get("customerId").textValue()).isEqualTo("1x1")
        assertDoesNotThrow { Instant.parse(consumedNode.get("createdAt").textValue()) }
        assertThat(consumedNode.get("stateOfCharge").doubleValue()).isEqualTo(1.0)
    }

    @Test
    fun `produces a drive stream of events and sends it to kafka`() {
        val expectedRepeat = 25
        webClient.post()
            .uri { uriBuilder ->
                uriBuilder
                    .path("/status/drive")
                    .queryParam("vin", "1")
                    .queryParam("stateOfCharge", 0.3)
                    .queryParam("stateOfChargeMin", 0.2)
                    .build()
            }
            .header("auth", "boomer")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isOk

        verifyBlocking(kafkaSenderService, times(expectedRepeat)) { send(any(), any(), any()) }
        verify(consumer, times(expectedRepeat)).receive(any())

        val argumentCaptor = argumentCaptor<ConsumerRecord<String, JsonNode>>()
        verify(consumer, atLeast(1)).receive(argumentCaptor.capture())
        val receivedEvents = argumentCaptor.allValues
        assertThat(receivedEvents.size).isEqualTo(expectedRepeat)
        assertTrue(receivedEvents.any { record -> record.value().get("stateOfCharge").doubleValue() >= 0.29 })
    }

    @Test
    fun `produces a charge stream of events and sends it to kafka`() {
        val expectedRepeat = 9
        webClient.post()
            .uri { uriBuilder ->
                uriBuilder
                    .path("/status/charge")
                    .queryParam("vin", "1")
                    .queryParam("stateOfChargeStart", 0.2)
                    .queryParam("stateOfChargeMax", 0.5)
                    .build()
            }
            .header("auth", "boomer")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isOk

        verifyBlocking(kafkaSenderService, times(expectedRepeat)) { send(any(), any(), any()) }
        verify(consumer, times(expectedRepeat)).receive(any())

        val argumentCaptor = argumentCaptor<ConsumerRecord<String, JsonNode>>()
        verify(consumer, atLeast(1)).receive(argumentCaptor.capture())
        val receivedEvents = argumentCaptor.allValues
        assertThat(receivedEvents.size).isEqualTo(expectedRepeat)
        assertTrue(receivedEvents.any { record -> record.value().get("stateOfCharge").doubleValue() == 0.5 })
    }

    @Test
    fun `a vehicle event sent is produced and consumed properly in kafka`() {
        val payload = """
                {
                  "vin": "1",
                  "details": {
                    "model": "Taycan",
                    "variant": "4S"
                  }
                }
            """.trimIndent()

        val argumentCaptor = argumentCaptor<ConsumerRecord<String, JsonNode>>()
        webClient.post()
            .uri("/vehicle")
            .header("auth", "boomer")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .body(
                BodyInserters.fromValue(payload)
            )
            .exchange()
            .expectStatus().isOk

        verify(consumer, timeout(timeout)).receive(argumentCaptor.capture())
        val consumedNode = argumentCaptor.firstValue.value()
        assertThat(consumedNode.get("vin").textValue()).isEqualTo("1")
        assertThat(consumedNode.get("details").toString()).isEqualTo("""{"model":"Taycan","variant":"4S"}""")
        assertDoesNotThrow { Instant.parse(consumedNode.get("updatedAt").textValue()) }
    }

    @Test
    fun `a customer event sent is produced and consumed properly in kafka`() {
        val payload = """
                {
                  "customerId": "1",
                  "profession": "Racer",
                  "age": 18
                }
            """.trimIndent()

        val argumentCaptor = argumentCaptor<ConsumerRecord<String, JsonNode>>()
        webClient.post()
            .uri("/customer")
            .header("auth", "boomer")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .body(
                BodyInserters.fromValue(payload)
            )
            .exchange()
            .expectStatus().isOk

        verify(consumer, timeout(timeout)).receive(argumentCaptor.capture())
        val consumedNode = argumentCaptor.firstValue.value()
        assertThat(consumedNode.get("customerId").textValue()).isEqualTo("1")
        assertThat(consumedNode.get("profession").textValue()).isEqualTo("Racer")
        assertThat(consumedNode.get("age").intValue()).isEqualTo(18)
        assertDoesNotThrow { Instant.parse(consumedNode.get("updatedAt").textValue()) }
    }

    @TestConfiguration
    class Support {

        @Component
        class KafkaTestConsumer {
            @KafkaListener(topics = ["vehicle-status", "vehicle-details", "customer-details"])
            fun receive(consumerRecord: ConsumerRecord<*, *>) {
            }
        }
    }
}