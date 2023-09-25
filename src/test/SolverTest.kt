import com.github.statnett.loadflowservice.defaultLoadFlowParameters
import com.powsybl.loadflow.LoadFlowParameters
import com.powsybl.loadflow.json.JsonLoadFlowParameters
import org.junit.Test
import java.io.ByteArrayInputStream
import kotlin.test.assertEquals

class SolverTest {
    @Test
    fun `test default parameter round trip`() {
        val parameters = LoadFlowParameters()
        val defaultJson = defaultLoadFlowParameters()
        val fromJson = JsonLoadFlowParameters.read(ByteArrayInputStream(defaultJson.toByteArray()))
        assertEquals(parameters.toString(), fromJson.toString())
    }

}