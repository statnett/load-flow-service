package com.github.statnett.loadflowservice

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import java.io.File

private val logger = KotlinLogging.logger {}

class Environment {
    companion object {
        var namespaceFile: String? = System.getenv("NAMESPACE_FILE")
        val cimResource: String? = System.getenv("CIM_RESOURCE")
    }
}

class Config {
    companion object {
        var namespaces: Map<String, String> = loadNamespaces(Environment.namespaceFile)
        val cimResource: String = Environment.cimResource ?: "CIM16.sparql"
    }
}

fun loadNamespaces(filename: String?): Map<String, String> {
    if (filename == null) {
        return mapOf()
    }

    val namespaceFile = File(filename)
    val jsonString = namespaceFile.readText()
    return Json.parseToJsonElement(jsonString).jsonObject.mapValues { item -> item.value.toString().replace("\"", "") }
}
