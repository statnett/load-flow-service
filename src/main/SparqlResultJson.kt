package com.github.statnett.loadflowservice

import kotlinx.serialization.Serializable

class SparqlTypes {
    companion object {
        const val URI = "uri"
        const val LITERAL = "literal"
    }
}

@Serializable
data class SparqlResultJson(
    val head: SparqlVars,
    val result: SparqlResult,
    val link: List<String> = listOf(),
)

@Serializable
data class SparqlVars(
    val vars: List<String>,
)

@Serializable
data class SparqlResult(
    val bindings: List<Map<String, SparqlItem>>,
)

@Serializable
data class SparqlItem(
    val type: String,
    val value: String,
    val dataType: String? = null,
)
