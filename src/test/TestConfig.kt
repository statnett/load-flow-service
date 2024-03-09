import com.github.statnett.loadflowservice.loadNamespaces
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertEquals

class TestConfig {
    @Test
    fun `test load namespaces from file`() {
        val filename = "temp_namespaces"
        val jsonString = "{\"cim\":\"c\",\"md\":\"m\"}"

        val file = File.createTempFile(filename, "json")
        file.deleteOnExit()
        file.writeText(jsonString)

        val result = loadNamespaces(file.path)

        assertEquals(mapOf("cim" to "c", "md" to "m"), result)
    }

    @Test
    fun `test load namespaces null`() {
        assertEquals(loadNamespaces(null), mapOf())
    }
}
