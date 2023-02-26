package com.mackie.streams.producer.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app")
data class AppConfiguration(
    val kafka: KafkaConfig
) {
    data class KafkaConfig(
        val clientId: String,
        val bootstrapServers: List<String>,
        val consumerGroupId: String,
        val partitions: Int = 3,
        val security: Security
    ) {
        data class Security(
            val enabled: Boolean,
            val username: String?,
            val password: String?
        )
    }
}