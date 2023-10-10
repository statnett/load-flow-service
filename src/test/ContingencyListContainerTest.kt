import com.github.statnett.loadflowservice.formItemHandlers.ContingencyListContainer
import com.powsybl.ieeecdf.converter.IeeeCdfNetworkFactory
import testDataFactory.basicContingencyJson
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class ContingencyListContainerTest {
    @Test
    fun `load basic json`() {
        val jsonString = basicContingencyJson()
        val container = ContingencyListContainer()
        container.update(jsonString)
        assertNotNull(container.contingencies)

        val network = IeeeCdfNetworkFactory.create9()
        val contingencies = container.contingencies.getContingencies(network)
        assertEquals(2, contingencies.size)
    }
}
