package com.github.statnett.loadflowservice

import com.powsybl.iidm.network.Network
import com.powsybl.loadflow.LoadFlowParameters
import com.powsybl.loadflow.json.JsonLoadFlowParameters
import com.powsybl.nad.NetworkAreaDiagram
import com.powsybl.sld.SingleLineDiagram
import io.ktor.http.content.*
import java.io.ByteArrayInputStream
import java.io.StringWriter

fun busesFromRequest(
    type: String,
    body: ByteArray,
): List<BusProperties> {
    val network = networkFromStream(type, ByteArrayInputStream(body))
    return busPropertiesFromNetwork(network)
}

class FileContent(val name: String, val bytes: ByteArray)

/**
 * Convenience class used to deserialize and update a load parameter instance
 */
class LoadParameterContainer {
    var parameters = LoadFlowParameters()
    private var parametersModified = false
    private fun update(jsonString: String) {
        this.parameters = JsonLoadFlowParameters.update(this.parameters, ByteArrayInputStream(jsonString.toByteArray()))
        this.parametersModified = true
    }

    fun formItemHandler(part: PartData.FormItem) {
        val name = part.name ?: ""
        if (name == "load-parameters") {
            this.update(part.value)
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
                val name = part.originalFileName as String
                val content = part.streamProvider().readBytes()
                files.add(FileContent(name, content))
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
        DiagramType.entries.first { item -> item.toString().lowercase() == value.lowercase() }
    } catch (e: NoSuchElementException) {
        DiagramType.Generic
    }

}

fun singleLineDiagram(type: DiagramType, name: String, network: Network): String {
    val svgWriter = StringWriter()

    // Declare a writer for metadata
    val metaDataWriter = StringWriter()
    when (type) {
        DiagramType.VoltageLevel -> {
            SingleLineDiagram.drawVoltageLevel(network, name, svgWriter, metaDataWriter)
        }

        DiagramType.Substation -> {
            SingleLineDiagram.drawSubstation(network, name, svgWriter, metaDataWriter)
        }

        DiagramType.Generic -> {
            SingleLineDiagram.draw(network, name, svgWriter, metaDataWriter)
        }
    }
    return svgWriter.toString()
}

fun networkDiagram(network: Network): String {
    val svgWriter = StringWriter()
    NetworkAreaDiagram(network).draw(svgWriter)
    return svgWriter.toString()
}

fun substationNames(network: Network): List<String> {
    return network.substations.map { substation -> substation.nameOrId }.toList()
}

fun voltageLevelNames(network: Network): List<String> {
    return network.voltageLevels.map { voltageLevel -> voltageLevel.nameOrId }.toList()
}