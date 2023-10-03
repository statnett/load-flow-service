package com.github.statnett.loadflowservice

import com.powsybl.iidm.network.Network
import com.powsybl.loadflow.LoadFlowParameters
import com.powsybl.loadflow.json.JsonLoadFlowParameters
import com.powsybl.nad.NetworkAreaDiagram
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

/**
 * Convenience class used to deserialize and update a load parameter instance
 */
class LoadParameterContainer {
    var parameters = LoadFlowParameters()
    private var parametersModified = false

    private fun currentVersion(): String {
        return LoadFlowParameters.VERSION
    }

    private fun addVersionToJsonString(jsonString: String): String {
        return "{\"version\": ${currentVersion()}," + jsonString.drop(1)
    }

    private fun hasVersion(jsonString: String): Boolean {
        return jsonString.contains("version")
    }

    private fun update(jsonString: String) {
        val jsonStringWithVersion = if (hasVersion(jsonString)) jsonString else addVersionToJsonString(jsonString)
        this.parameters = JsonLoadFlowParameters.update(this.parameters, jsonStringWithVersion.byteInputStream())
        this.parametersModified = true
    }

    fun formItemHandler(part: PartData.FormItem) {
        val name = part.name ?: ""
        if (name == "load-flow-parameters") {
            this.update(part.value)
            logger.info { "Received load flow parameters: ${part.value}" }
        }
    }
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