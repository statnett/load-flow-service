import com.powsybl.ieeecdf.converter.IeeeCdfNetworkFactory
import io.ktor.client.request.*
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.statement.*
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import java.io.File
import java.nio.file.Paths
import java.util.Properties
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
                    url = "/get-buses",
                    formData =
                        formData {
                            append("network", "not file content")
                        },
                )
            assertEquals(HttpStatusCode.UnprocessableEntity, response.status)
        }

    @Test
    fun `test receive 14 buses for 14 bus network`() =
        testApplication {
            // Initialize temporary file
            val file = File.createTempFile("network", ".xiidm")
            file.deleteOnExit()

            IeeeCdfNetworkFactory.create14().write("XIIDM", Properties(), Paths.get(file.path))

            val response =
                client.submitFormWithBinaryData(
                    url = "/get-buses",
                    formData =
                        formData {
                            append(
                                "network",
                                file.readBytes(),
                                Headers.build {
                                    append(HttpHeaders.ContentDisposition, "filename=${file.name}")
                                },
                            )
                        },
                )
            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals(response.headers["Content-Type"], "application/json; charset=UTF-8")
            val body: String = response.bodyAsText()

            // Roughly validate contant
            assertTrue(body.startsWith("[{"))
            assertTrue(body.endsWith("}]"))

            val busString = "{\"id\":\"VL1_0\",\"voltage\":143.1,\"angle\":0.0,\"activePower\":0.0,\"reactivePower\":0.0}"
            assertContains(body, busString)
        }
}
