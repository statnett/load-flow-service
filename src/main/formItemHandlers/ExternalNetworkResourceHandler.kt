package com.github.statnett.loadflowservice.formItemHandlers

import com.github.statnett.loadflowservice.Config
import com.github.statnett.loadflowservice.SparqlResultJson
import com.github.statnett.loadflowservice.createExtractionQuery
import com.github.statnett.loadflowservice.parseQuery
import com.github.statnett.loadflowservice.updateInMemTripleStore
import com.powsybl.triplestore.api.PropertyBags
import com.powsybl.triplestore.impl.rdf4j.TripleStoreRDF4J
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.http.HttpHeaders
import io.ktor.http.append
import io.ktor.http.content.PartData
import io.ktor.server.util.url
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json

private val logger = KotlinLogging.logger(ExternalNetworkResourceHandler::class.simpleName ?: "")

class ExternalNetworkResourceHandler(
    private val authorizationHeader: String? = null,
    private val cimResource: String = Config.cimResource,
    httpEngine: HttpClientEngine? = null,
) : FormItemLoadable {
    private val client: HttpClient =
        HttpClient(httpEngine ?: CIO.create()) {
            expectSuccess = true
        }

    private val tripleStore = TripleStoreRDF4J()

    override fun formItemHandler(part: PartData.FormItem) {
        val name = part.name ?: ""
        if (name == FormItemNames.NETWORK) {
            val externalResourceUrls = part.value.split(",")
            val sparqlResults = collectExternalNetworkData(externalResourceUrls)
            sparqlResults.forEach { updateInMemTripleStore(tripleStore, it) }
        }
    }

    private fun collectExternalNetworkData(urls: List<String>): List<SparqlResultJson> {
        val parsedQuery = parseQuery(cimResource)
        val extractionQuery = createExtractionQuery(parsedQuery)
        return runBlocking { makeRequests(urls, extractionQuery) }
    }

    private suspend fun makeRequests(
        urls: List<String>,
        query: String,
    ) = coroutineScope {
        urls.map { url ->
            async(Dispatchers.IO) {
                makeSparqlRequest(url, query)
            }
        }.awaitAll()
    }

    private suspend fun makeSparqlRequest(
        resourceUrl: String,
        query: String,
    ): SparqlResultJson {
        logger.info { "Requesting data from $resourceUrl" }
        val response =
            client.get(resourceUrl) {
                url {
                    parameters.append("query", query)
                }
                headers {
                    if (authorizationHeader != null) {
                        append(HttpHeaders.Authorization, authorizationHeader)
                    }
                    append(HttpHeaders.Accept, "application/sparql-results+json")
                }
            }
        return Json.decodeFromString<SparqlResultJson>(response.body())
    }

    // Used for testing
    fun tripleStoreQuery(query: String): PropertyBags = tripleStore.query(query)
}
