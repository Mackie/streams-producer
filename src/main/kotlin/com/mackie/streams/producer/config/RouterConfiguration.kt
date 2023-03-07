package com.mackie.streams.producer.config

import com.mackie.streams.producer.handler.CustomerEventRequestHandler
import com.mackie.streams.producer.handler.StatusEventRequestHandler
import com.mackie.streams.producer.handler.VehicleEventRequestHandler
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.reactive.config.WebFluxConfigurer
import org.springframework.web.reactive.function.server.*

@Configuration
class RouterConfiguration : WebFluxConfigurer {

    @Value("\${app.password}")
    lateinit var authPassword: String

    @Bean
    fun statusRouter(
        statusHandler: StatusEventRequestHandler,
        vehicleHandler: VehicleEventRequestHandler,
        customerHandler: CustomerEventRequestHandler
    ): RouterFunction<ServerResponse> = coRouter {
        filter(authFilter(authPassword))
        contentType(MediaType.APPLICATION_JSON)
        POST("/status", statusHandler::createStatusEvent)
        POST("/status/drive", statusHandler::createDriveEvents)
        POST("/status/charge", statusHandler::createChargeEvents)
        POST("/vehicle", vehicleHandler::createVehicleEvent)
        POST("/customer", customerHandler::createVehicleEvent)
    }

    fun authFilter(pw: String): suspend (ServerRequest, (suspend (ServerRequest) -> ServerResponse)) -> ServerResponse =
        { request, handler ->
            val header = request.headers().firstHeader("auth")
            if (header != null) {
                handler(request)
            } else ServerResponse.status(HttpStatus.UNAUTHORIZED).buildAndAwait()
        }
}