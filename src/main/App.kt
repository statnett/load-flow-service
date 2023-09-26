package com.github.statnett.loadflowservice

import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.plugins.swagger.*

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

fun Application.module() {
    install(ContentNegotiation) {
        json()
    }

    routing {
        get("/") {
            call.respondText("Hello, world!")
        }

        post("/get-buses") {
            val files = multiPartDataHandler(call.receiveMultipart())

            if (files.isEmpty()) {
                call.response.status(HttpStatusCode.UnprocessableEntity)
            } else {
                val busProps = busesFromRequest(files[0].name, files[0].bytes)
                call.respond(busProps)
            }
        }

        get("/default-load-parameters") {
            call.respondText(defaultLoadFlowParameters(), ContentType.Application.Json, HttpStatusCode.OK)
        }

        post("/run-load-flow") {
            val paramContainer = LoadParameterContainer()
            val files = multiPartDataHandler(call.receiveMultipart(), paramContainer::formItemHandler)

            if (files.isEmpty()) {
                call.response.status(HttpStatusCode.UnprocessableEntity)
            } else {
                val network = networkFromFileContent(files[0])
                val result = solve(network, paramContainer.parameters)
                call.respond(result)
            }
        }
        swaggerUI(path="openapi", swaggerFile = "openapi/documentation.yaml")
    }
}
