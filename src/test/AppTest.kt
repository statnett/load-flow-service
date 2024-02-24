import com.github.statnett.loadflowservice.*
import com.github.statnett.loadflowservice.formItemHandlers.FormItemNames.Companion.LOAD_FLOW_PARAMS
import com.powsybl.ieeecdf.converter.IeeeCdfNetworkFactory
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import testDataFactory.*
import testUtils.retryOnError
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ApplicationTest {
    // Holds various form data variants for the sensitivity-analysis end-point
    private val sensitivityFormData = SensitivityAnalysisFormDataContainer()
    private val securityFormData = SecurityAnalysisFormDataContainer()

    private val json = Json {
        ignoreUnknownKeys = true
    }


    @Test
    fun testRoot() =
        testApplication {
            val response = client.get("/")
            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals("Hello, world!", response.bodyAsText())
        }


    @TestFactory
    fun `test missing network in form`() = listOf(
        "/run-load-flow",
        "/object-names/substations",
        "/object-names/voltage-levels",
        "/diagram",
        "/diagram/substation/S1",
        "/diagram/voltage-level/VL1",
        "/object-names/generators",
        "/object-names/branches",
        "/object-names/loads",
        "/object-names/buses"
    ).map { url ->
        DynamicTest.dynamicTest("422 when no network is passed to $url") {
            testApplication {
                val response = client.submitFormWithBinaryData(url = url, formData = listOf())
                assertEquals(HttpStatusCode.UnprocessableEntity, response.status)
            }
        }
    }

    @TestFactory
    fun `test internal server error when file parsing fails`() = listOf(
        "/run-load-flow",
        "/object-names/substations",
        "/diagram",
        "/diagram/substation/S1",
        "/diagram/voltage-level/VL1"
    ).map { url ->
        DynamicTest.dynamicTest("500 when file content can not be parsed $url") {
            testApplication {
                val response = client.submitFormWithBinaryData(url = url, formData = formDataWithEmptyNetwork())
                assertEquals(HttpStatusCode.InternalServerError, response.status)
                val body = response.bodyAsText()
                assertTrue(body.contains("PowsyblException"))
            }
        }
    }

    @Test
    fun `test receive 14 buses for 14 bus network`() =
        testApplication {
            val response =
                client.submitFormWithBinaryData(
                    url = "/object-names/buses",
                    formData = formDataFromFile(ieeeCdfNetwork14File()),
                )
            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals(response.headers["Content-Type"], "application/json; charset=UTF-8")
            val names = json.decodeFromString<List<String>>(response.body())

            assertEquals(14, names.size)
        }

    @Test
    fun `test default load parameters`() =
        testApplication {
            val response = client.get("/default-values/$LOAD_FLOW_PARAMS")
            assertEquals(response.status, HttpStatusCode.OK)

            val body: String = response.bodyAsText()
            assertTrue(body.startsWith("{"))
            assertTrue(body.endsWith("}"))
        }

    @Test
    fun `test flow 14 bus network ok`() =
        testApplication {
            val response = client.submitFormWithBinaryData(
                url = "/run-load-flow",
                formData = formDataFromFile((ieeeCdfNetwork14File()))
            )

            assertEquals(response.status, HttpStatusCode.OK)
            val taskInfo = Json.decodeFromString<TaskInfo>(response.body())

            val statusResponse = client.get(taskInfo.statusUrl)
            assertEquals(HttpStatusCode.OK, statusResponse.status)

            val resultResponse = retryOnError(50, 20) {
                client.get(taskInfo.resultUrl)
            }
            assertEquals(resultResponse.status, HttpStatusCode.OK)

            val result = json.decodeFromString<LoadFlowResultForApi>(resultResponse.body())

            val solvedNetwork = IeeeCdfNetworkFactory.create14Solved()
            val angles = busPropertiesFromNetwork(solvedNetwork).map { bus -> bus.angle }.toList()

            val anglesFromJsonStr = result.buses.map { bus -> bus.angle }.toList()

            // It seems like the solved version from Powsybl contains rounded angles
            assertTrue(
                angles.zip(anglesFromJsonStr).all { pair ->
                    abs(pair.component1() - pair.component2()) < 0.01
                }
            )
        }

    @Test
    fun `test 14 bus ok`() =
        testApplication {
            val response = client.submitFormWithBinaryData(
                url = "/object-names/buses",
                formData = formDataFromFile(ieeeCdfNetwork14CgmesFile())
            )
            assertEquals(HttpStatusCode.OK, response.status)
        }

    @Test
    fun `test dc solve ok`() =
        testApplication {
            val formData = formDataFromFile(ieeeCdfNetwork14File())
            val loadParams = formData {
                append("load-flow-parameters", "{\"dc\": true}")
            }

            val response = client.submitFormWithBinaryData(
                url = "/run-load-flow",
                formData = formData + loadParams
            )

            val taskInfo = json.decodeFromString<TaskInfo>(response.body())

            val statusResponse = client.get(taskInfo.statusUrl)
            assertEquals(HttpStatusCode.OK, statusResponse.status)
            val status = json.decodeFromString<TaskStatusResponse>(statusResponse.body())
            assertEquals("RUNNING", status.status)

            val resultResponse = retryOnError(50, 10) {
                client.get(taskInfo.resultUrl)
            }
            assertEquals(HttpStatusCode.OK, resultResponse.status)

            val apiResult = json.decodeFromString<LoadFlowResultForApi>(resultResponse.body())
            assertEquals(HttpStatusCode.OK, response.status)
            assertTrue(apiResult.isOk)
        }

    @Test
    fun `test descriptive response on incompatible version of load parameters`() =
        testApplication {
            val formData = formDataFromFile(ieeeCdfNetwork14File())
            val loadParams = formData {
                append("load-flow-parameters", "{\"version\":\"1.0\",\"dc\": true}")
            }

            val response = client.submitFormWithBinaryData(
                url = "/run-load-flow",
                formData = formData + loadParams
            )
            val body = response.bodyAsText()
            assertEquals(HttpStatusCode.InternalServerError, response.status)

            // dc flag was introduced in v1.4
            assertTrue(body.contains(">= 1.4"))
        }

    @Test
    fun `test response ok network`() =
        testApplication {
            val response = client.submitFormWithBinaryData(
                url = "/diagram",
                formData = formDataFromFile(ieeeCdfNetwork14File())
            )
            val body = response.bodyAsText()
            assertEquals(ContentType.Image.SVG.toString(), response.headers["Content-Type"])
            assertTrue(isPlausibleSvg(body))
            assertEquals(response.status, HttpStatusCode.OK)
        }

    @Test
    fun `test bad request when substation does not exist`() =
        testApplication {
            val response = client.submitFormWithBinaryData(
                url = "/diagram/substation/non-existent-station",
                formData = formDataFromFile(ieeeCdfNetwork14File())
            )
            assertEquals(HttpStatusCode.InternalServerError, response.status)
            val body = response.bodyAsText()
            assertTrue(body.contains("Substation 'non-existent-station' not found"))
        }

    @Test
    fun `test response OK and svg produced substation`() =
        testApplication {
            val response = client.submitFormWithBinaryData(
                url = "/diagram/substation/S1",
                formData = formDataFromFile(ieeeCdfNetwork14File())
            )
            assertEquals(response.status, HttpStatusCode.OK)
            val body = response.bodyAsText()
            assertTrue(isPlausibleSvg(body))
        }

    @Test
    fun `test 11 substation names extracted`() =
        testApplication {
            val response = client.submitFormWithBinaryData(
                url = "/object-names/substations",
                formData = formDataFromFile(ieeeCdfNetwork14File())
            )
            assertEquals(response.status, HttpStatusCode.OK)
            val substationNames = response.bodyAsText().split(",")
            assertEquals(substationNames.size, 11)
        }

    @Test
    fun `test 14 voltage levels extracted`() =
        testApplication {
            val response = client.submitFormWithBinaryData(
                url = "/object-names/voltage-levels",
                formData = formDataFromFile(ieeeCdfNetwork14File())
            )
            assertEquals(response.status, HttpStatusCode.OK)
            val voltageLevels = response.bodyAsText().split(",")
            assertEquals(14, voltageLevels.size)
        }

    @Test
    fun `test svg produced for voltage level`() =
        testApplication {
            val response = client.submitFormWithBinaryData(
                url = "/diagram/voltageLevel/VL1",
                formData = formDataFromFile(ieeeCdfNetwork14File())
            )
            assertEquals(response.status, HttpStatusCode.OK)
            val body = response.bodyAsText()
            assertTrue(isPlausibleSvg(body))
            assertEquals(response.headers["Content-Type"].toString(), "image/svg+xml")
        }

    @Test
    fun `test read rawx`() =
        testApplication {
            val response = client.submitFormWithBinaryData(
                url = "/object-names/buses",
                formData = formDataMinimalNetworkRawx()
            )
            assertEquals(HttpStatusCode.OK, response.status)
            val names = json.decodeFromString<List<String>>(response.body())
            assertEquals(2, names.size)
        }

    @TestFactory
    fun `test 200 response for all valid sensitivity inputs`() = allSensitivityAnalysisConfigs().map { config ->
        DynamicTest.dynamicTest("Test 200 for config $config") {
            testApplication {
                val response = client.submitFormWithBinaryData(
                    url = "/sensitivity-analysis",
                    formData = sensitivityFormData.formData(config)
                )
                assertEquals(HttpStatusCode.OK, response.status)

                val taskInfo = Json.decodeFromString<TaskInfo>(response.body())

                val runResult = retryOnError(50, 10) {
                    client.get(taskInfo.resultUrl)
                }
                assertEquals(HttpStatusCode.OK, runResult.status)

                val result = json.decodeFromString<LoadFlowServiceSensitivityAnalysisResult>(runResult.body())

                // There are two contingencies so when we have contingencies there should be three results
                // Otherwise one
                val numRes = if (config.withContingencies) 3 else 1

                assertEquals(numRes, result.sensitivityAnalysisResult.values.size)
            }
        }
    }

    @TestFactory
    fun `test response 200 and that known substring is part of the body`() = listOf(
        mapOf("url" to "/object-names/generators", "content-substring" to "B1-G"),
        mapOf("url" to "/object-names/loads", "content-substring" to "B2-L"),
        mapOf("url" to "/object-names/branches", "content-substring" to "L7-8-1")
    ).map { args ->
        DynamicTest.dynamicTest("Test ${args["url"]}") {
            testApplication {
                val response = client.submitFormWithBinaryData(
                    url = args["url"]!!,
                    formData = sensitivityFormData.network
                )

                assertEquals(HttpStatusCode.OK, response.status)
                val body = response.bodyAsText()
                assertTrue(body.contains(args["content-substring"]!!))
                assertEquals("application/json; charset=UTF-8", response.headers["content-type"])
            }
        }
    }

    @Test
    fun `test response 200 and some known content for default sensitivity parameters`() {
        testApplication {
            val response = client.get("/default-values/sensitivity-analysis-params")
            assertEquals(HttpStatusCode.OK, response.status)
            val body = response.bodyAsText()
            assertTrue(body.contains("flow-voltage-sensitivity-value-threshold"))
        }
    }

    @Test
    fun `test response 404 on unknown parameter set`() {
        testApplication {
            val response = client.get("/default-values/non-existing-params")
            assertEquals(HttpStatusCode.NotFound, response.status)
            val body = response.bodyAsText()
            assertTrue(body.contains("UnknownRoute"))
        }
    }

    @Test
    fun `test 404 response on unknown object type`() {
        testApplication {
            val response = client.submitFormWithBinaryData(
                url = "/object-names/non-existing-object-type",
                formData = sensitivityFormData.network
            )

            assertEquals(HttpStatusCode.NotFound, response.status)
            val body = response.bodyAsText()
            assertTrue(body.contains("UnknownRoute"))
        }
    }

    @Test
    fun `test default security analysis parameters`() {
        testApplication {
            val response = client.get("default-values/security-analysis-params")
            assertEquals(HttpStatusCode.OK, response.status)
            val body = response.bodyAsText()
            assertTrue(body.startsWith("{"))
            assertTrue(body.endsWith("}"))
        }

    }

    @Test
    fun `test 200 for simple security analysis`() {
        testApplication {
            val response = client.submitFormWithBinaryData(
                url = "/security-analysis",
                formData = securityFormData.formData()
            )
            assertEquals(HttpStatusCode.OK, response.status)
            val taskInfo = Json.decodeFromString<TaskInfo>(response.body())

            val runResult = retryOnError(50, 10) {
                client.get(taskInfo.resultUrl)
            }
            assertEquals(HttpStatusCode.OK, runResult.status)

            val result = json.decodeFromString<LoadFlowServiceSecurityAnalysisResult>(runResult.body())
            assertTrue(result.report.isNotEmpty())
            // There are two contingencies
            assertEquals(2, result.securityAnalysisResult.postContingencyResults.size)
        }
    }


}


// Function for checking some properties of a body to verify that the returned body
// is a valid svg image
fun isPlausibleSvg(body: String): Boolean {
    return body.contains("<svg") && body.contains("<?xml version")
}


