import com.github.statnett.loadflowservice.formItemHandlers.ExternalNetworkResourceHandler
import com.github.statnett.loadflowservice.formItemHandlers.FormItemNames
import io.kotest.property.arbitrary.take
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockEngineConfig
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.http.headersOf
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import testDataFactory.SmallCimModels
import testDataFactory.sparqlResultArb
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

const val UNAVAILABLE_URL = "http://unavailable"
const val AVAILABLE_URL = "http://available"
const val DESERIALIZATION_ERROR = "http://random-json"
const val TOKEN_AUTHORIZATION_REQUIRED = "http://secret-sparql"
const val SECRET_TOKEN = "secret-token"
const val TWO_TERMINALS_URL = "http://two-terminals"
const val TWO_CONNECTIVITY_NODES_URL = "http://two-connectivity-nodes"
const val CIM = "http://cim-prefix"

fun buildFormItem(
    value: String,
    name: String,
): PartData.FormItem {
    return PartData.FormItem(value, {}, headersOf(HttpHeaders.ContentDisposition, "form-data; name=$name"))
}

class ExternalNetworkResourceHandlerTest {
    private val mockEngineConfig = MockEngineConfig()

    init {
        mockEngineConfig.addHandler { request ->
            val urlStr = request.url.toString()
            if (urlStr.startsWith(UNAVAILABLE_URL)) {
                respond(
                    "Not available",
                    HttpStatusCode.ServiceUnavailable,
                    headersOf(HttpHeaders.ContentType, "text/plain"),
                )
            } else if (urlStr.startsWith(AVAILABLE_URL)) {
                respond(
                    Json.encodeToString(sparqlResultArb.take(1).first()),
                    HttpStatusCode.OK,
                    headersOf(HttpHeaders.ContentType, "application/sparql-results+json"),
                )
            } else if (urlStr.startsWith(DESERIALIZATION_ERROR)) {
                respond(
                    """{"field": 1.0}""",
                    HttpStatusCode.OK,
                    headersOf(HttpHeaders.ContentType, "application/json"),
                )
            } else if (urlStr.startsWith(TOKEN_AUTHORIZATION_REQUIRED)) {
                val token = request.headers[HttpHeaders.Authorization]
                respond(
                    Json.encodeToString(sparqlResultArb.take(1).first()),
                    if (token == SECRET_TOKEN) HttpStatusCode.OK else HttpStatusCode.Unauthorized,
                    headersOf(HttpHeaders.ContentType, "application/sparql-results+json"),
                )
            } else if (urlStr.startsWith(TWO_TERMINALS_URL)) {
                respond(
                    Json.encodeToString(SmallCimModels(CIM).twoTerminalsWithConnectivityNode()),
                    HttpStatusCode.OK,
                    headersOf(HttpHeaders.ContentType, "application/sparql-results+json"),
                )
            } else if (urlStr.startsWith(TWO_CONNECTIVITY_NODES_URL)) {
                respond(
                    Json.encodeToString(SmallCimModels(CIM).twoConnectivityNodes()),
                    HttpStatusCode.OK,
                    headersOf(HttpHeaders.ContentType, "application/sparql-results+json"),
                )
            } else {
                throw IllegalArgumentException("${request.url} not known")
            }
        }
    }

    @TestFactory
    fun `test error on unavailable`() =
        listOf(
            UNAVAILABLE_URL,
            "$UNAVAILABLE_URL,$UNAVAILABLE_URL",
            "$AVAILABLE_URL,$UNAVAILABLE_URL",
        ).map { urls ->
            DynamicTest.dynamicTest(urls) {
                val data = buildFormItem(urls, FormItemNames.NETWORK)
                assertEquals(FormItemNames.NETWORK, data.name!!)

                val engine = MockEngine(mockEngineConfig)
                val handler = ExternalNetworkResourceHandler(httpEngine = engine)
                assertFailsWith<ServerResponseException> { handler.formItemHandler(data) }
            }
        }

    @Test
    fun `test serialization error raises`() {
        val data = buildFormItem(DESERIALIZATION_ERROR, FormItemNames.NETWORK)
        val engine = MockEngine(mockEngineConfig)
        assertFailsWith<SerializationException> {
            ExternalNetworkResourceHandler(
                httpEngine = engine,
            ).formItemHandler(data)
        }
    }

    @TestFactory
    fun `test available ok with random data`() =
        listOf(
            AVAILABLE_URL,
            "$AVAILABLE_URL,$AVAILABLE_URL",
        ).map { urls ->
            DynamicTest.dynamicTest(urls) {
                val data = buildFormItem(urls, FormItemNames.NETWORK)
                val engine = MockEngine(mockEngineConfig)
                ExternalNetworkResourceHandler(httpEngine = engine).formItemHandler(data)
            }
        }

    @TestFactory
    fun `test authentication error`() =
        listOf(
            null,
            "wrong-token",
        ).map { token ->
            DynamicTest.dynamicTest("$token") {
                val data = buildFormItem(TOKEN_AUTHORIZATION_REQUIRED, FormItemNames.NETWORK)
                val engine = MockEngine(mockEngineConfig)
                assertFailsWith<ClientRequestException> {
                    ExternalNetworkResourceHandler(
                        httpEngine = engine,
                        authorizationHeader = token,
                    ).formItemHandler(data)
                }
            }
        }

    @Test
    fun `test authentication ok`() {
        val data = buildFormItem(TOKEN_AUTHORIZATION_REQUIRED, FormItemNames.NETWORK)
        val engine = MockEngine(mockEngineConfig)
        ExternalNetworkResourceHandler(httpEngine = engine, authorizationHeader = SECRET_TOKEN).formItemHandler(data)
    }

    @Test
    fun `test store populated with small cim model`() {
        val data = buildFormItem("$TWO_TERMINALS_URL,$TWO_CONNECTIVITY_NODES_URL", FormItemNames.NETWORK)
        val engine = MockEngine(mockEngineConfig)
        val handler = ExternalNetworkResourceHandler(httpEngine = engine)
        handler.formItemHandler(data)

        val query =
            """
            PREFIX cim: <$CIM#>
            
            SELECT ?name {
                ?terminal cim:Terminal.ConnectivityNode/cim:IdentifiedObject.name ?name
                }
            """.trimIndent()
        val result = handler.tripleStoreQuery(query)
        assertEquals(2, result.size)
        val expect = setOf("Connectivity node 1", "Connectivity node 2")
        val got = result.map { item -> item["name"] }.toSet()
        assertEquals(expect, got)
    }
}
