import com.github.statnett.loadflowservice.parseQuery
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

class TestExternalTripleStore {
    @Test
    fun `test predicate extraction`() {
        val queryContent = parseQuery("CIM16.sparql")
        val expect = setOf("a", "cim:ConnectivityNode.TopologicalNode", "cim:IdentifiedObject.name")
        assertTrue { queryContent.predicates.containsAll(expect) }
        assertTrue { queryContent.prefixes.isNotEmpty() }
    }
}
