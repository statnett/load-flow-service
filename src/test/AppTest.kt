import com.github.statnett.loadflowservice.busPropertiesFromNetwork
import com.powsybl.ieeecdf.converter.IeeeCdfNetworkFactory
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.testing.*
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import java.io.File
import java.nio.file.Paths
import java.util.*
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ApplicationTest {
    @Test
    fun testRoot() =
        testApplication {
            val response = client.get("/")
            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals("Hello, world!", response.bodyAsText())
        }

    @Test
    fun `test get buses returns 422 on missing file content`() =
        testApplication {
            val response =
                client.submitFormWithBinaryData(
                    url = "/buses",
                    formData =
                    formData {
                        append("network", "not file content")
                    },
                )
            assertEquals(HttpStatusCode.UnprocessableEntity, response.status)
        }

    @TestFactory
    fun `test missing network in form`() = listOf(
        "/buses",
        "/run-load-flow",
        "/substation-names",
        "/voltage-level-names",
        "/diagram",
        "/diagram/substation/S1",
        "/diagram/voltage-level/VL1"
    ).map { url ->
        DynamicTest.dynamicTest("422 when no network is passed to $url") {
            testApplication {
                val response = client.submitFormWithBinaryData(url = url, formData = listOf())
                assertEquals(HttpStatusCode.UnprocessableEntity, response.status)
            }
        }
    }

    @TestFactory
    fun `test internal server error when file parsing fails`()  = listOf(
        "/buses",
        "/run-load-flow",
        "/substation-names",
        "/voltage-level-names",
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
                    url = "/buses",
                    formData = formDataFromFile(ieeeCdfNetwork14File()),
                )
            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals(response.headers["Content-Type"], "application/json; charset=UTF-8")
            val body: String = response.bodyAsText()

            // Roughly validate content
            assertTrue(body.startsWith("[{"))
            assertTrue(body.endsWith("}]"))

            val busString =
                "{\"id\":\"VL1_0\",\"voltage\":143.1,\"angle\":0.0,\"activePower\":0.0,\"reactivePower\":0.0}"
            assertContains(body, busString)
        }

    @Test
    fun `test default load parameters`() =
        testApplication {
            val response = client.get("/default-load-parameters")
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

            val body: String = response.bodyAsText()
            val solvedNetwork = IeeeCdfNetworkFactory.create14Solved()
            val angles = busPropertiesFromNetwork(solvedNetwork).map { bus -> bus.angle }.toList()

            val regex = Regex("\"angle\":([0-9.-]+)")
            val anglesFromJsonStr = regex.findAll(body).map { match -> match.groupValues[1].toDouble() }.toList()

            // It seems like the solved version from Powsybl contains rounded angles
            assertTrue(
                angles.zip(anglesFromJsonStr).all { pair ->
                    abs(pair.component1() - pair.component2()) < 0.01
                }
            )
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
                url = "/substation-names",
                formData = formDataFromFile(ieeeCdfNetwork14File())
            )
            assertEquals(response.status, HttpStatusCode.OK)
            val substationNames = response.bodyAsText().split(",")
            assertEquals(substationNames.size, 11)
        }

    @Test
    fun `test 2 voltage levels extracted`() =
        testApplication {
            val response = client.submitFormWithBinaryData(
                url = "/voltage-level-names",
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
}

fun formDataFromFile(file: File): List<PartData> {
    return formData {
        append(
            "network",
            file.readBytes(),
            Headers.build {
                append(HttpHeaders.ContentDisposition, "filename=${file.name}")
            },
        )
    }
}

fun formDataWithEmptyNetwork(): List<PartData> {
    return formData {
        append(
            "network",
            byteArrayOf(),
            Headers.build {
                append(HttpHeaders.ContentDisposition, "filename=emptyFile.xiidm")
            }
        )
    }
}

fun ieeeCdfNetwork14File(): File {
    // Initialize temporary file
    val file = File.createTempFile("network", ".xiidm")
    file.deleteOnExit()

    IeeeCdfNetworkFactory.create14().write("XIIDM", Properties(), Paths.get(file.path))
    return file
}

// Function for checking some properties of a body to verify that the returned body
// is a valid svg image
fun isPlausibleSvg(body: String): Boolean {
    return body.contains("<svg") && body.contains("<?xml version")
}