package com.github.statnett.loadflowservice

import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.plugins.swagger.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

fun Application.module() {
    install(ContentNegotiation) {
        json()
    }

    install(CORS) {
        anyHost()
        allowHeader(HttpHeaders.ContentType)
    }

    install(StatusPages) {
        exception<Throwable> { call, cause ->
            call.respondText(
                "500: $cause.\nStack trace: ${cause.stackTraceToString()}",
                status = HttpStatusCode.InternalServerError
            )
        }
    }

    install(CallLogging)

    routing {
        get("/") {
            call.respondText("Hello, world!")
        }

        post("/buses") {
            val files = multiPartDataHandler(call.receiveMultipart())

            if (files.isEmpty()) {
                call.response.status(HttpStatusCode.UnprocessableEntity)
            } else {
                val busProps = busesFromRequest(files[0])
                call.respond(busProps)
            }
        }

        get("/default-load-parameters") {
            call.respondText(defaultLoadFlowParameters(), ContentType.Application.Json, HttpStatusCode.OK)
        }

        post("/substation-names") {
            val files = multiPartDataHandler(call.receiveMultipart())
            if (files.isEmpty()) {
                call.response.status(HttpStatusCode.UnprocessableEntity)
            } else {
                val network = networkFromFileContent(files[0])
                call.respond(substationNames(network))
            }
        }

        post("/voltage-level-names") {
            val files = multiPartDataHandler(call.receiveMultipart())
            if (files.isEmpty()) {
                call.response.status(HttpStatusCode.UnprocessableEntity)
            } else {
                val network = networkFromFileContent(files[0])
                call.respond(voltageLevelNames(network))
            }
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

        post("/diagram/{type}/{name}") {
            val diagramType = getDiagramType(call.parameters["type"] ?: DiagramType.Generic.toString())
            val name = call.parameters["name"] ?: ""
            val files = multiPartDataHandler((call.receiveMultipart()))
            if (files.isEmpty()) {
                call.response.status(HttpStatusCode.UnprocessableEntity)
            } else {
                val network = networkFromFileContent(files[0])
                val diagram = singleLineDiagram(diagramType, name, network)
                call.respondText(diagram, ContentType.Image.SVG, HttpStatusCode.OK)
            }
        }

        post("/diagram") {
            val files = multiPartDataHandler((call.receiveMultipart()))
            if (files.isEmpty()) {
                call.response.status(HttpStatusCode.UnprocessableEntity)
            } else {
                val network = networkFromFileContent(files[0])
                val diagram = networkDiagram(network)
                call.respondText(diagram, ContentType.Image.SVG, HttpStatusCode.OK)
            }
        }
        swaggerUI(path = "openapi", swaggerFile = "openapi/documentation.yaml")
    }
}
