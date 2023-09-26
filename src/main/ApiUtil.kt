package com.github.statnett.loadflowservice

import com.powsybl.loadflow.LoadFlowParameters
import com.powsybl.loadflow.json.JsonLoadFlowParameters
import io.ktor.http.content.*
import java.io.ByteArrayInputStream

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
