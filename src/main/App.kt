package com.github.statnett.loadflowservice

import com.github.statnett.loadflowservice.formItemHandlers.*
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
            ExceptionHandler().handle(call, cause)
        }
    }

    install(CallLogging)

    routing {
        get("/") {
            call.respondText("Hello, world!")
        }

        post("/buses") {
            val files = multiPartDataHandler(call.receiveMultipart())
            val network = networkFromFirstFile(files)
            call.respond(busPropertiesFromNetwork(network))
        }

        get("/default-values/{parameter-set}") {
            val parameterSet = call.parameters["parameter-set"] ?: ""
            call.respondText(defaultParameterSet(parameterSet))
        }

        post("/object-names/{type}") {
            val files = multiPartDataHandler(call.receiveMultipart())
            val network = networkFromFirstFile(files)
            val type = call.parameters["type"] ?: ""
            call.respond(modelObjectNames(type, network))
        }

        post("/run-load-flow") {
            val paramContainer = LoadParameterContainer()
            val files = multiPartDataHandler(call.receiveMultipart(), paramContainer::formItemHandler)
            val network = networkFromFirstFile(files)
            val result = solve(network, paramContainer.parameters)
            call.respond(result)
        }

        post("/diagram/{type}/{name}") {
            val diagramType = getDiagramType(call.parameters["type"] ?: DiagramType.Generic.toString())
            val name = call.parameters["name"] ?: ""
            val files = multiPartDataHandler((call.receiveMultipart()))

            val network = networkFromFirstFile(files)
            val diagram = singleLineDiagram(diagramType, name, network)
            call.respondText(diagram, ContentType.Image.SVG, HttpStatusCode.OK)

        }

        post("/diagram") {
            val files = multiPartDataHandler((call.receiveMultipart()))
            val network = networkFromFirstFile(files)
            val diagram = networkDiagram(network)
            call.respondText(diagram, ContentType.Image.SVG, HttpStatusCode.OK)
        }

        post("/sensitivity-analysis") {
            val loadParamCnt = LoadParameterContainer()
            val sensParamCnt = SensitivityAnalysisParametersContainer()
            val sensFactorCnt = SensitivityFactorContainer()
            val contingencyCnt = ContingencyListContainer()
            val itemHandler = MultiFormItemLoaders(listOf(loadParamCnt, sensParamCnt, sensFactorCnt, contingencyCnt))

            val files = multiPartDataHandler(call.receiveMultipart(), itemHandler::formItemHandler)

            sensParamCnt.parameters.setLoadFlowParameters(loadParamCnt.parameters)
            val network = networkFromFirstFile(files)
            val result = runSensitivityAnalysis(
                network,
                sensFactorCnt.factors,
                sensParamCnt.parameters,
                contingencyCnt.contingencies
            )
            call.respondText(result, ContentType.Application.Json, HttpStatusCode.OK)
        }
        swaggerUI(path = "openapi", swaggerFile = "openapi/documentation.yaml")
    }
}
