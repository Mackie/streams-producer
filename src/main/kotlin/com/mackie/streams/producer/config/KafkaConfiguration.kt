package com.mackie.streams.producer.config

import com.fasterxml.jackson.databind.JsonNode
import com.mackie.streams.producer.config.json.serde.JacksonSerdeDeserializer
import com.mackie.streams.producer.config.json.serde.JacksonSerdeSerializer
import io.micrometer.core.instrument.MeterRegistry
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.admin.NewTopic
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.config.SaslConfigs
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.*

@Configuration
class KafkaConfiguration(
    val appConfiguration: AppConfiguration
) {

    enum class Topics(val topicName: String) {
        VEHICLE_MAIN("vehicle-status"),
        VEHICLE_DETAILS("vehicle-details"),
        CUSTOMER("customer-details");
    }

    @Bean
    fun mainTopic(): NewTopic = NewTopic(Topics.VEHICLE_MAIN.topicName, appConfiguration.kafka.partitions, 1)

    @Bean
    fun vehicleTopic(): NewTopic = NewTopic(Topics.VEHICLE_DETAILS.topicName, appConfiguration.kafka.partitions, 1)

    @Bean
    fun customerTopic(): NewTopic = NewTopic(Topics.CUSTOMER.topicName, appConfiguration.kafka.partitions, 1)

    @Bean
    fun kafkaAdmin(): KafkaAdmin {
        return KafkaAdmin(commonKafkaConfig())
    }

    @Bean
    fun producerFactory(meterRegistry: MeterRegistry): ProducerFactory<String, JsonNode> {
        val producerFactory = DefaultKafkaProducerFactory<String, JsonNode>(commonKafkaConfig().plus(producerConfig()))
        producerFactory.addListener(MicrometerProducerListener(meterRegistry))
        return producerFactory
    }

    @Bean
    fun kafkaTemplate(producerFactory: ProducerFactory<String, JsonNode>): KafkaTemplate<String, JsonNode> {
        return KafkaTemplate(producerFactory)
    }


    @Bean
    fun consumerFactory(): ConsumerFactory<String, JsonNode> {
        return DefaultKafkaConsumerFactory(commonKafkaConfig().plus(consumerConfig()))
    }

    @Bean
    fun kafkaListenerContainerFactory(consumerFactory: ConsumerFactory<String, JsonNode>): ConcurrentKafkaListenerContainerFactory<String, JsonNode> {
        val factory = ConcurrentKafkaListenerContainerFactory<String, JsonNode>()
        factory.consumerFactory = consumerFactory
        return factory
    }

    fun commonKafkaConfig(): MutableMap<String, Any> {
        val props: MutableMap<String, Any> = HashMap()
        props[CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG] = appConfiguration.kafka.bootstrapServers.joinToString(",")
        props[CommonClientConfigs.CLIENT_ID_CONFIG] = appConfiguration.kafka.clientId

        if (appConfiguration.kafka.security.enabled) {
            props[CommonClientConfigs.SECURITY_PROTOCOL_CONFIG] = "SASL_SSL"
            props[SaslConfigs.SASL_MECHANISM] = "SCRAM-SHA-512"
            props[SaslConfigs.SASL_JAAS_CONFIG] =
                config(appConfiguration.kafka.security.username, appConfiguration.kafka.security.username)
        }
        return props
    }

    fun producerConfig(): MutableMap<String, Any> {
        val props: MutableMap<String, Any> = HashMap()
        props[ConsumerConfig.GROUP_ID_CONFIG] = appConfiguration.kafka.consumerGroupId
        props[ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java
        props[ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG] = JacksonSerdeSerializer::class.java
        return props
    }

    fun consumerConfig(): MutableMap<String, Any> {
        val props: MutableMap<String, Any> = HashMap()
        props[ConsumerConfig.GROUP_ID_CONFIG] = appConfiguration.kafka.consumerGroupId
        props[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java
        props[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = JacksonSerdeDeserializer::class.java
        return props
    }

    fun config(username: String?, password: String?) =
        "org.apache.kafka.common.security.scram.ScramLoginModule required username=\"$username\" password=\"$password\";"

}