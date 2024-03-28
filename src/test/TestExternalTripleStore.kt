import com.github.statnett.loadflowservice.ParsedSparqlQuery
import com.github.statnett.loadflowservice.SparqlItem
import com.github.statnett.loadflowservice.SparqlTypes
import com.github.statnett.loadflowservice.createExtractionQuery
import com.github.statnett.loadflowservice.insertTriple
import com.github.statnett.loadflowservice.parseQuery
import com.github.statnett.loadflowservice.populateInMemTripleStore
import com.powsybl.triplestore.impl.rdf4j.TripleStoreRDF4J
import io.kotest.common.ExperimentalKotest
import io.kotest.property.PropTestConfig
import io.kotest.property.forAll
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import testDataFactory.sparqlResultArb
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TestExternalTripleStore {
    @Test
    fun `test predicate extraction`() {
        val queryContent = parseQuery("CIM16.sparql")
        val expect = setOf("rdf:type", "cim:ConnectivityNode.TopologicalNode", "cim:IdentifiedObject.name")
        assertTrue { queryContent.predicates.containsAll(expect) }
        assertTrue { queryContent.prefixes.isNotEmpty() }
    }

    @Test
    fun `test create extraction query`() {
        val parsedQuery =
            ParsedSparqlQuery(
                mutableMapOf("md" to "<http://md>", "cim" to "<http://cim>"),
                setOf("cim:a", "md:b"),
            )

        val expectedQuery = (
            "PREFIX md: <http://md>\nPREFIX cim: <http://cim>\nSELECT ?graph ?s ?p ?o {\n" +
                "VALUES ?p { cim:a md:b }\nGRAPH ?graph {?s ?p ?o}}"
        )
        assertEquals(createExtractionQuery(parsedQuery), expectedQuery)
    }

    @Test
    fun `test insert query literal`() {
        val result =
            mapOf(
                "graph" to SparqlItem(SparqlTypes.URI, "g"),
                "s" to SparqlItem(SparqlTypes.URI, "s"),
                "p" to SparqlItem(SparqlTypes.URI, "p"),
                "o" to SparqlItem(SparqlTypes.LITERAL, "0.0"),
            )
        val query = insertTriple(result)
        val expect = "GRAPH <g> {<s> <p> \"0.0\"}"
        assertEquals(expect, query)
    }

    @Test
    fun `test insert query object`() {
        val result =
            mapOf(
                "graph" to SparqlItem(SparqlTypes.URI, "g"),
                "s" to SparqlItem(SparqlTypes.URI, "s"),
                "p" to SparqlItem(SparqlTypes.URI, "p"),
                "o" to SparqlItem(SparqlTypes.URI, "o"),
            )
        val query = insertTriple(result)
        val expect = "GRAPH <g> {<s> <p> <o>}"
        assertEquals(expect, query)
    }

    @TestFactory
    fun `test insert query null when any null`() =
        listOf(
            listOf("graph", "s", "p"),
            listOf("graph", "s", "o"),
            listOf("graph", "p", "o"),
            listOf("s", "p", "o"),
            listOf(),
        ).map {
            DynamicTest.dynamicTest("Test null") {
                val result = it.associateWith { field -> SparqlItem(SparqlTypes.URI, field) }
                assertNull(insertTriple(result))
            }
        }

    @OptIn(ExperimentalKotest::class)
    @Test
    fun `test populate triple store`() =
        runBlocking {
            val result =
                forAll(PropTestConfig(iterations = 100), sparqlResultArb) {
                    val store = populateInMemTripleStore(it)
                    val cnt = store.query("SELECT (COUNT(*) as ?cnt) WHERE {graph ?g {?s ?p ?o}}")[0]["cnt"]!!.toInt()
                    cnt == it.result.bindings.size
                }
        }

    @Test
    fun `test extraction query works as expected`() {
        val store = TripleStoreRDF4J()
        val query =
            """
            PREFIX cim: <http://iec.ch/TC57/2013/CIM-schema-cim16#>
            
            INSERT DATA {
                GRAPH <http://g> {
                    _:b0 cim:ConnectivityNode.TopologicalNode _:b1 .
                    _:b0 cim:IdentifiedObject.name "Connectivity node" .
                    _:b1 cim:IdentifiedObject.name "Topological node" .
                    _:b1 cim:SomeRandomNonExistentPredicate.name "random predicate"
                }
            }
            """.trimIndent()
        store.update(query)

        // Confirm 4 triples were inserted
        val cnt = store.query("SELECT (count(*) as ?cnt) where {graph ?g {?s ?p ?o}}")[0]["cnt"]!!.toInt()
        assertEquals(4, cnt)

        val parsedQuery = parseQuery("CIM16.sparql")

        // Update with a few prefixes not found in the CIM16.sparql file
        parsedQuery.prefixes["cim"] = "<http://iec.ch/TC57/2013/CIM-schema-cim16#>"
        parsedQuery.prefixes["entsoe"] = "<http://entsoe>"
        val result = store.query(createExtractionQuery(parsedQuery))

        // Expect three records to be extracted
        assertEquals(3, result.size)
    }
}
