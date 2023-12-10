package com.github.statnett.loadflowservice

import com.fasterxml.jackson.databind.ObjectMapper
import com.powsybl.iidm.network.Network
import com.powsybl.nad.NetworkAreaDiagram
import com.powsybl.security.SecurityAnalysisParameters
import com.powsybl.security.json.SecurityAnalysisJsonModule
import com.powsybl.sensitivity.SensitivityAnalysisParameters
import com.powsybl.sensitivity.json.SensitivityJsonModule
import com.powsybl.sld.SingleLineDiagram
import com.powsybl.sld.SldParameters
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.content.*
import java.io.StringWriter

private val logger = KotlinLogging.logger {}

fun busesFromRequest(
    content: FileContent
): List<BusProperties> {
    val network = networkFromFileContent(content)
    return busPropertiesFromNetwork(network)
}

suspend fun multiPartDataHandler(
    multiPartData: MultiPartData,
    formItemHandler: (part: PartData.FormItem) -> Unit = {}
): List<FileContent> {
    val files = mutableListOf<FileContent>()
    multiPartData.forEachPart { part ->
        when (part) {
            is PartData.FormItem -> {
                formItemHandler(part)
            }

            is PartData.FileItem -> {
                val name = part.originalFileName ?: ""
                val content = part.streamProvider().readBytes()
                val fileContent = FileContent(name, content)
                logger.info { "Received file $name with size ${content.size} bytes. Content hash: ${fileContent.contentHash()}" }
                files.add(fileContent)
            }

            else -> {}
        }
        part.dispose()
    }
    return files
}

fun undoPrettyPrintJson(jsonString: String): String {
    return jsonString.replace("\n", "").replace(" ", "")
}

enum class DiagramType { Generic, Substation, VoltageLevel }

// Case insensitive enum value matching
fun getDiagramType(value: String): DiagramType {
    return try {
        DiagramType.entries.first { item -> item.toString().lowercase() == value.replace("-", "").lowercase() }
    } catch (e: NoSuchElementException) {
        DiagramType.Generic
    }

}

fun singleLineDiagram(type: DiagramType, name: String, network: Network): String {
    val svgWriter = StringWriter()

    // Declare a writer for metadata
    val metaDataWriter = StringWriter()
    val sldParams = SldParameters()
    when (type) {
        DiagramType.VoltageLevel -> {
            SingleLineDiagram.drawVoltageLevel(network, name, svgWriter, metaDataWriter, sldParams)
        }

        DiagramType.Substation -> {
            SingleLineDiagram.drawSubstation(network, name, svgWriter, metaDataWriter, sldParams)
        }

        DiagramType.Generic -> {
            SingleLineDiagram.draw(network, name, svgWriter, metaDataWriter)
        }
    }
    return svgWriter.toString()
}

fun networkDiagram(network: Network): String {
    val svgWriter = StringWriter()
    NetworkAreaDiagram.draw(network, svgWriter)
    return svgWriter.toString()
}

fun substationNames(network: Network): List<String> {
    return network.substations.map { substation -> substation.nameOrId }.toList()
}

fun voltageLevelNames(network: Network): List<String> {
    return network.voltageLevels.map { voltageLevel -> voltageLevel.nameOrId }.toList()
}

fun generatorNames(network: Network): List<String> {
    return network.generators.map { generator -> generator.nameOrId }
}

fun loadNames(network: Network): List<String> {
    return network.loads.map { load -> load.nameOrId }
}

fun branchNames(network: Network): List<String> {
    return network.lines.map { line -> line.nameOrId }
}

fun busNames(network: Network): List<String> {
    return network.busView.buses.map { bus -> bus.nameOrId }
}

fun defaultSensitivityAnalysisParameters(): String {
    val mapper = ObjectMapper()
    mapper.registerModule(SensitivityJsonModule())
    return mapper.writeValueAsString(SensitivityAnalysisParameters())
}

fun defaultSecurityAnalysisParameters(): String {
    val mapper = ObjectMapper()
    mapper.registerModule(SecurityAnalysisJsonModule())
    return mapper.writeValueAsString(SecurityAnalysisParameters())
}

class UnknownRouteException(message: String) : Exception(message)

fun defaultParameterSet(name: String): String {
    val loadParams = "load-params"
    val sensitivityAnalysisParams = "sensitivity-analysis-params"
    val securityAnalysisParams = "security-analysis-params"
    return when (name) {
        loadParams -> {
            defaultLoadFlowParameters()
        }

        sensitivityAnalysisParams -> {
            defaultSensitivityAnalysisParameters()
        }

        securityAnalysisParams -> {
            defaultSecurityAnalysisParameters()
        }

        else -> {
            val allowed = listOf(loadParams, sensitivityAnalysisParams)
            throw UnknownRouteException("Unknown parameters set $name. Must be one of $allowed")
        }
    }
}

fun modelObjectNames(name: String, network: Network): List<String> {
    val substation = "substations"
    val voltageLevel = "voltage-levels"
    val generators = "generators"
    val loads = "loads"
    val branches = "branches"
    val buses = "buses"
    return when (name) {
        substation -> {
            substationNames(network)
        }

        voltageLevel -> {
            voltageLevelNames(network)
        }

        generators -> {
            generatorNames(network)
        }

        loads -> {
            loadNames(network)
        }

        branches -> {
            branchNames(network)
        }

        buses -> {
            busNames(network)
        }

        else -> {
            val allowed = listOf(substation, voltageLevel, generators, loads, branches, buses)
            throw UnknownRouteException("Unknown object type $name. Must be one of $allowed")
        }
    }
}