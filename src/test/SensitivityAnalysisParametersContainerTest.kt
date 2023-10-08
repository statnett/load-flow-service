import com.github.statnett.loadflowservice.SensitivityAnalysisParametersContainer
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue

class SensitivityAnalysisParametersContainerTest {
    @Test
    fun `load basic json`() {
        val jsonString = "{\"voltage-voltage-sensitivity-value-threshold\": 1234.0}"
        val container = SensitivityAnalysisParametersContainer()
        container.update(jsonString)

        assertTrue(abs(container.parameters.voltageVoltageSensitivityValueThreshold - 1234.0) < 1e-8)
    }
}
