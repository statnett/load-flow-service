import com.github.statnett.loadflowservice.LoadFlowServiceSecurityAnalysisResult
import com.github.statnett.loadflowservice.busPropertiesFromNetwork
import com.powsybl.ieeecdf.converter.IeeeCdfNetworkFactory
import com.powsybl.security.SecurityAnalysisResult
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class ApiDataModelTest {
    @Test
    fun `Should be 14 buses in test network`() {
        val network = IeeeCdfNetworkFactory.create14()
        val buses = busPropertiesFromNetwork(network)
        assertEquals(buses.count(), 14)
    }

    @Test
    fun `serialize deserialize round trip should be give the same security analysis report`() {
        val emptyReport = SecurityAnalysisResult.empty()
        val result = LoadFlowServiceSecurityAnalysisResult(emptyReport, "some run report")
        val serialized = Json.encodeToString(result)
        val deserialized = Json.decodeFromString<LoadFlowServiceSecurityAnalysisResult>(serialized)
        assertEquals(deserialized.report, result.report)
    }
}
