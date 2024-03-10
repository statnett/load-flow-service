import com.github.statnett.loadflowservice.ParsedSparqlQuery
import com.github.statnett.loadflowservice.Quad
import com.github.statnett.loadflowservice.createExtractionQuery
import com.github.statnett.loadflowservice.parseQuery
import com.github.statnett.loadflowservice.populateTripleStore
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

    @Test
    fun `test populate triplestore`() {
        val quads =
            listOf(
                Quad("<http://g>", "<urn:uuid:a1>", "ns:a", "ns:b"),
                Quad("<http://g>", "<urn:uuid:a2>", "ns:a.x", "2.0"),
            )

        val prefixes = mapOf("ns" to "<http://ns.com>")
        val store = populateTripleStore(quads, prefixes)
        assertEquals(setOf("http://g"), store.contextNames())

        val result = store.query("select (count(*) as ?count) where {?s ?p ?o}")
        assertEquals(2, result[0]["count"]!!.toInt())
    }
}
