package com.mackie.streams.producer.handler

import com.mackie.streams.producer.sender.VehicleEventSender
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.awaitBody
import org.springframework.web.reactive.function.server.buildAndAwait

@Service
class VehicleEventRequestHandler(
    private val vehicleEventSender: VehicleEventSender
) {
    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    suspend fun createVehicleEvent(request: ServerRequest): ServerResponse =
        runCatching {
            vehicleEventSender.send(request.awaitBody())
            ServerResponse.ok().buildAndAwait()
        }.getOrDefault(ServerResponse.badRequest().buildAndAwait())
}