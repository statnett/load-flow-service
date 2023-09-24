import com.github.statnett.loadflowservice.busPropertiesFromNetwork
import com.powsybl.ieeecdf.converter.IeeeCdfNetworkFactory
import kotlin.test.Test
import kotlin.test.assertEquals

class ApiDataModelTest {
    @Test
    fun `Should be 14 buses in test network`() {
        val network = IeeeCdfNetworkFactory.create14()
        val buses = busPropertiesFromNetwork(network)
        assertEquals(buses.count(), 14)
    }
}
