package com.mackie.streams.producer.handler

import com.mackie.streams.producer.sender.StatusEventSender
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.awaitBody
import org.springframework.web.reactive.function.server.buildAndAwait

@Service
class StatusEventRequestHandler(
    private val mainEventSender: StatusEventSender
) {
    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    suspend fun createStatusEvent(request: ServerRequest): ServerResponse =
        runCatching {
            mainEventSender.send(request.awaitBody())
            ServerResponse.ok().buildAndAwait()
        }.getOrDefault(ServerResponse.badRequest().buildAndAwait())

    suspend fun createDriveEvents(request: ServerRequest): ServerResponse =
        runCatching {
            mainEventSender.sendDriveEvents(
                request.queryParam("vin").get(),
                request.queryParam("stateOfCharge").get().toDouble(),
                request.queryParam("stateOfChargeMin").get().toDouble()
            )
            ServerResponse.ok().buildAndAwait()
        }.getOrDefault(ServerResponse.badRequest().buildAndAwait())

    suspend fun createChargeEvents(request: ServerRequest): ServerResponse =
        runCatching {
            mainEventSender.sendChargeEvents(
                request.queryParam("vin").get(),
                request.queryParam("stateOfChargeStart").get().toDouble(),
                request.queryParam("stateOfChargeMax").get().toDouble()
            )
            ServerResponse.ok().buildAndAwait()
        }.getOrDefault(ServerResponse.badRequest().buildAndAwait())
}