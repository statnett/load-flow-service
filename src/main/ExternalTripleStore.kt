package com.github.statnett.loadflowservice

import com.powsybl.triplestore.api.QueryCatalog

data class ParsedSparqlQuery(
    val prefixes: Map<String, String>,
    val predicates: Set<String>,
)

fun parseQuery(sparqlQueryResource: String): ParsedSparqlQuery {
    val catalog = QueryCatalog(sparqlQueryResource)

    return ParsedSparqlQuery(
        extractPrefixes(catalog),
        extractPredicates(catalog),
    )
}

fun extractPredicates(catalog: QueryCatalog): Set<String> {
    val regexpPattern = Regex("([a-zA-Z0-9.]*:[a-zA-Z0-9.]+)")
    val matches = catalog.values.map { query -> regexpPattern.findAll(query) }.asSequence().flatten()
    val extraPredicates = setOf("a")
    return matches.map { match -> match.value }.toSet().union(extraPredicates)
}

fun extractPrefixes(catalog: QueryCatalog): Map<String, String> {
    val regex = Regex("PREFIX|prefix ([0-9a-zA-Z]+):.*(<[0-9a-zA-Z:/#.-]+>)")
    val matches = catalog.values.map { query -> regex.findAll(query) }.asSequence().flatten()
    return matches.map { match -> Pair(match.groupValues[0], match.groupValues[1]) }.toMap()
}
