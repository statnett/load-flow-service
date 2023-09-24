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

suspend fun fileContentFromRequest(multiPartData: MultiPartData): FileContent {
    var name = ""
    var content = byteArrayOf()
    multiPartData.forEachPart { part ->
        when (part) {
            is PartData.FileItem -> {
                name = part.originalFileName as String
                content = part.streamProvider().readBytes()
            }

            else -> {}
        }
        part.dispose()
    }
    return FileContent(name, content)
}
