package com.github.statnett.loadflowservice

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*

class ExceptionHandler {
    suspend fun handle(call: ApplicationCall, cause: Throwable) {
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
                    status = HttpStatusCode.NotFound
                )
            }

            else -> {
                call.respondText(
                    "500: $cause.\nStack trace: ${cause.stackTraceToString()}",
                    status = HttpStatusCode.InternalServerError
                )
            }
        }
    }
}