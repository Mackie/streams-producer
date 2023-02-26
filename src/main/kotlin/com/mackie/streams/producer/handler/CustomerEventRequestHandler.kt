package com.mackie.streams.producer.handler

import com.mackie.streams.producer.sender.CustomerEventSender
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.awaitBody
import org.springframework.web.reactive.function.server.buildAndAwait

@Service
class CustomerEventRequestHandler(
    private val customerEventSender: CustomerEventSender
) {
    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    suspend fun createVehicleEvent(request: ServerRequest): ServerResponse =
        runCatching {
            customerEventSender.send(request.awaitBody())
            ServerResponse.ok().buildAndAwait()
        }.getOrDefault(ServerResponse.badRequest().buildAndAwait())
}