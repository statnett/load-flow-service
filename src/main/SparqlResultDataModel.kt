package com.github.statnett.loadflowservice

import kotlinx.serialization.Serializable

@Serializable
data class SparqlResult(
    val head: SparqlHead,
    val results: Results,
)

@Serializable
data class SparqlHead(val vars: List<String>)

@Serializable
data class Results(val bindings: List<Map<String, ValueItem>>)

@Serializable
data class ValueItem(val type: String, val value: String, val datatype: String? = null)

data class Quad(val graph: String, val subject: String, val predicate: String, val obj: String)
