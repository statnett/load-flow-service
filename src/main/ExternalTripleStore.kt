package com.github.statnett.loadflowservice

import com.powsybl.triplestore.api.QueryCatalog
import com.powsybl.triplestore.impl.rdf4j.TripleStoreRDF4J

data class ParsedSparqlQuery(
    val prefixes: MutableMap<String, String>,
    val predicates: Set<String>,
)

fun parseQuery(sparqlQueryResource: String): ParsedSparqlQuery {
    val catalog = QueryCatalog(sparqlQueryResource)

    val namespaces: MutableMap<String, String> = mutableMapOf()
    namespaces.putAll(Config.namespaces)
    namespaces.putAll(extractPrefixes(catalog))
    return ParsedSparqlQuery(
        namespaces,
        extractPredicates(catalog),
    )
}

fun extractPredicates(catalog: QueryCatalog): Set<String> {
    val regexpPattern = Regex("([a-zA-Z0-9.]*:[a-zA-Z0-9.]+)")
    val matches = catalog.values.map { query -> regexpPattern.findAll(query) }.asSequence().flatten()
    val extraPredicates = setOf("rdf:type")
    return matches.map { match -> match.value }.toSet().union(extraPredicates)
}

fun extractPrefixes(catalog: QueryCatalog): Map<String, String> {
    val regex = Regex("PREFIX|prefix ([0-9a-zA-Z]+):.*(<[0-9a-zA-Z:/#.-]+>)")
    val matches = catalog.values.map { query -> regex.findAll(query) }.asSequence().flatten()
    return matches.map { match -> Pair(match.groupValues[1], match.groupValues[2]) }.toMap()
}

fun createExtractionQuery(query: ParsedSparqlQuery): String {
    val prefix = query.prefixes.map { entry -> "PREFIX ${entry.key}: ${entry.value}" }.joinToString(separator = "\n")
    val predicates = query.predicates.joinToString(separator = " ")
    val select = "SELECT ?graph ?s ?p ?o {\nVALUES ?p { $predicates }\nGRAPH ?graph {?s ?p ?o}}"
    return "$prefix\n$select"
}

fun populateInMemTripleStore(sparqlResult: SparqlResultJson): TripleStoreRDF4J {
    val store = TripleStoreRDF4J()
    store.update(insertQuery(sparqlResult.result.bindings))
    return store
}

fun updateInMemTripleStore(
    store: TripleStoreRDF4J,
    sparqlResult: SparqlResultJson,
) {
    store.update(insertQuery(sparqlResult.result.bindings))
}

fun insertTriple(result: Map<String, SparqlItem>): String? {
    val graph = result["graph"]
    val subject = result["s"]
    val predicate = result["p"]
    val obj = result["o"]

    if ((graph == null) || (subject == null) || (predicate == null) || (obj == null)) {
        return null
    }
    var insertStatement = "GRAPH <${graph.value}> {<${subject.value}> <${predicate.value}> "
    insertStatement +=
        if (obj.type == SparqlTypes.URI) {
            "<${obj.value}>"
        } else {
            "\"${obj.value}\""
        }
    insertStatement += "}"
    return insertStatement
}

fun insertQuery(results: List<Map<String, SparqlItem>>): String {
    val triples = results.mapNotNull { insertTriple(it) }.joinToString(" .\n")
    return "INSERT DATA {$triples}"
}
