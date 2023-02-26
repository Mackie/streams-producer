package com.mackie.streams.producer

import com.mackie.streams.producer.config.AppConfiguration
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication

@SpringBootApplication
@EnableConfigurationProperties(AppConfiguration::class)
class StreamProducerApplication

fun main(args: Array<String>) {
    runApplication<StreamProducerApplication>(*args)
}
