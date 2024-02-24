package com.github.statnett.loadflowservice

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respondText

class ExceptionHandler {
    suspend fun handle(
        call: ApplicationCall,
        cause: Throwable,
    ) {
        when (cause) {
            is NoFileProvidedException -> {
                call.respondText(
                    "$cause\nStack trace: ${cause.stackTraceToString()}",
                    status = HttpStatusCode.UnprocessableEntity,
                )
            }

            is UnknownRouteException -> {
                call.respondText(
                    "$cause",
                    status = HttpStatusCode.NotFound,
                )
            }

            is TaskDoesNotExistException -> {
                call.respondText(
                    "$cause",
                    status = HttpStatusCode.NotFound,
                )
            }

            is FullBufferException -> {
                call.respondText(
                    "Service is currently unavailable because too many tasks are running.",
                    status = HttpStatusCode.ServiceUnavailable,
                )
            }

            else -> {
                call.respondText(
                    "500: $cause. Stack trace: ${cause.stackTraceToString()}",
                    status = HttpStatusCode.InternalServerError,
                )
            }
        }
    }
}
