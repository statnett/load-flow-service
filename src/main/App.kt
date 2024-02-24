package com.github.statnett.loadflowservice

import com.github.statnett.loadflowservice.formItemHandlers.ContingencyListContainer
import com.github.statnett.loadflowservice.formItemHandlers.LoadParameterContainer
import com.github.statnett.loadflowservice.formItemHandlers.MultiFormItemLoaders
import com.github.statnett.loadflowservice.formItemHandlers.SecurityAnalysisParametersContainer
import com.github.statnett.loadflowservice.formItemHandlers.SensitivityAnalysisParametersContainer
import com.github.statnett.loadflowservice.formItemHandlers.SensitivityFactorContainer
import com.powsybl.security.action.Action
import com.powsybl.security.interceptors.SecurityAnalysisInterceptor
import com.powsybl.security.monitor.StateMonitor
import com.powsybl.security.strategy.OperatorStrategy
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.callloging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.plugins.swagger.swaggerUI
import io.ktor.server.request.receiveMultipart
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

fun Application.module() {
    val taskManager = TaskManager()
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
            val result = createTask(taskManager) { solve(network, paramContainer.parameters) }
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
            val result =
                createTask(taskManager) {
                    runSensitivityAnalysis(
                        network,
                        sensFactorCnt.factors,
                        sensParamCnt.parameters,
                        contingencyCnt.contingencies,
                    )
                }
            call.respond(result)
        }

        post("/security-analysis") {
            val loadParamCnt = LoadParameterContainer()
            val securityParamsCnt = SecurityAnalysisParametersContainer()
            val contingencyCnt = ContingencyListContainer()
            val itemHandler = MultiFormItemLoaders(listOf(loadParamCnt, securityParamsCnt, contingencyCnt))

            val files = multiPartDataHandler(call.receiveMultipart(), itemHandler::formItemHandler)

            securityParamsCnt.parameters.setLoadFlowParameters(loadParamCnt.parameters)
            val network = networkFromFirstFile(files)

            // Initialize preliminary empty things in first version
            val intersceptors: List<SecurityAnalysisInterceptor> = listOf()
            val operatorStrategies: List<OperatorStrategy> = listOf()
            val actions: List<Action> = listOf()
            val monitors: List<StateMonitor> = listOf()

            val result =
                createTask(taskManager) {
                    runSecurityAnalysis(
                        network,
                        securityParamsCnt.parameters,
                        contingencyCnt,
                        intersceptors,
                        operatorStrategies,
                        actions,
                        monitors,
                    )
                }
            call.respond(result)
        }

        get("/status/{id}") {
            val id = call.parameters["id"] ?: ""
            call.respond(taskManager.status(id))
        }

        get("/result/{id}") {
            val id = call.parameters["id"] ?: ""
            taskManager.respondWithResult(call, id)
        }
        swaggerUI(path = "docs", swaggerFile = "openapi/documentation.yaml")
    }
}
