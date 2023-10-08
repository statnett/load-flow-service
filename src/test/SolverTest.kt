import com.github.statnett.loadflowservice.FileContent
import com.github.statnett.loadflowservice.defaultLoadFlowParameters
import com.github.statnett.loadflowservice.networkFromFileContent
import com.powsybl.loadflow.LoadFlowParameters
import com.powsybl.loadflow.json.JsonLoadFlowParameters
import org.junit.Test
import testDataFactory.ieeeCdfNetwork14CgmesFile
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

    @Test
    fun `test load network from zip input stream`() {
        val cimXmlFile = ieeeCdfNetwork14CgmesFile()
        val fc = FileContent("cgmes_network.zip", cimXmlFile.readBytes())
        val network = networkFromFileContent(fc)
        assertEquals(14, network.busView.buses.toList().size)
    }
}