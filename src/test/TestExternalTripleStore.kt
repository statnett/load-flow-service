import com.github.statnett.loadflowservice.ParsedSparqlQuery
import com.github.statnett.loadflowservice.createExtractionQuery
import com.github.statnett.loadflowservice.parseQuery
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TestExternalTripleStore {
    @Test
    fun `test predicate extraction`() {
        val queryContent = parseQuery("CIM16.sparql")
        val expect = setOf("a", "cim:ConnectivityNode.TopologicalNode", "cim:IdentifiedObject.name")
        assertTrue { queryContent.predicates.containsAll(expect) }
        assertTrue { queryContent.prefixes.isNotEmpty() }
    }

    @Test
    fun `test create extraction query`() {
        val parsedQuery =
            ParsedSparqlQuery(
                mapOf("md" to "<http://md>", "cim" to "<http://cim>"),
                setOf("cim:a", "md:b"),
            )

        val expectedQuery = (
            "PREFIX md: <http://md>\nPREFIX cim: <http://cim>\nSELECT ?graph ?s ?p ?o {\n" +
                "VALUES ?p { cim:a md:b }\nGRAPH ?graph {?s ?p ?o}}"
        )
        assertEquals(createExtractionQuery(parsedQuery), expectedQuery)
    }
}
