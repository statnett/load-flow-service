import com.github.statnett.loadflowservice.formItemHandlers.SensitivityFactorContainer
import testDataFactory.sensitivityFactorList
import kotlin.test.Test
import kotlin.test.assertEquals

class SensitivityFactorContainerTest {
    @Test
    fun `load basic json`() {
        val container = SensitivityFactorContainer()
        container.update(sensitivityFactorList())
        assertEquals(1, container.factors.size)
    }
}
