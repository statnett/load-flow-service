package com.github.statnett.loadflowservice

import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.http.content.streamProvider
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

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
            var fileName = ""
            var fileBytes = byteArrayOf()

            val multiPartData = call.receiveMultipart()

            multiPartData.forEachPart { part ->
                when (part) {
                    is PartData.FileItem -> {
                        fileName = part.originalFileName as String
                        fileBytes = part.streamProvider().readBytes()
                    }

                    else -> {}
                }
                part.dispose()
            }

            if (fileName == "") {
                call.response.status(HttpStatusCode.UnprocessableEntity)
            } else {
                val busProps = busesFromRequest(fileName, fileBytes)
                call.respond(busProps)
            }
        }
    }
}
