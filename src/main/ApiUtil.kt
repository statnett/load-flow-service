package com.github.statnett.loadflowservice

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

suspend fun multiPartDataHandler(multiPartData: MultiPartData): List<FileContent> {
    val files = mutableListOf<FileContent>()
    multiPartData.forEachPart { part ->
        when (part) {
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
