package com.mackie.streams.producer.handler

import com.mackie.streams.producer.sender.StatusEventSender
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.convert.DurationStyle
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

    suspend fun createRandomStatusEvents(request: ServerRequest): ServerResponse =
        runCatching {
            mainEventSender.sendRandom(
                request.awaitBody(),
                request.queryParam("repeats").get().toInt(),
                DurationStyle.detectAndParse(request.queryParam("frequency").get()),
            )
            ServerResponse.ok().buildAndAwait()
        }.getOrDefault(ServerResponse.badRequest().buildAndAwait())
}