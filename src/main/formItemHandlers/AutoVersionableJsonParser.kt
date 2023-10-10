package com.github.statnett.loadflowservice.formItemHandlers

import io.ktor.http.content.*

abstract class AutoVersionableJsonParser {
    internal fun addVersionToJsonString(jsonString: String): String {
        return "{\"version\": ${currentVersion()}," + jsonString.drop(1)
    }

    internal fun hasVersion(jsonString: String): Boolean {
        return jsonString.contains("version")
    }

    internal fun jsonStringWithVersion(jsonString: String): String {
        return if (hasVersion(jsonString)) jsonString else addVersionToJsonString(jsonString)
    }

    abstract fun currentVersion(): String
    abstract fun formItemHandler(part: PartData.FormItem)
}