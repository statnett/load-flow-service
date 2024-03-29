package testDataFactory

import com.github.statnett.loadflowservice.SparqlItem
import com.github.statnett.loadflowservice.SparqlResult
import com.github.statnett.loadflowservice.SparqlResultJson
import com.github.statnett.loadflowservice.SparqlTypes
import com.github.statnett.loadflowservice.SparqlVars
import io.kotest.property.Arb
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.choice
import io.kotest.property.arbitrary.constant
import io.kotest.property.arbitrary.distinct
import io.kotest.property.arbitrary.float
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.orNull
import io.kotest.property.arbitrary.string
import io.kotest.property.arbitrary.stringPattern
import io.kotest.property.arbitrary.uuid

val sparqlResultArb =
    arbitrary {
        val vars = listOf("graph", "s", "p", "o")
        val items =
            Arb.list(
                arbitrary {
                    mapOf(
                        "graph" to sparqlGraphArb.bind(),
                        "s" to sparqlUuidArb.bind(),
                        "p" to sparqlPredicateArb.bind(),
                        "o" to sparqlObjectArb.bind(),
                    )
                },
            )
        SparqlResultJson(
            SparqlVars(vars),
            result = SparqlResult(items.bind().distinct()),
        )
    }

val sparqlItemArb =
    arbitrary {
        val type = Arb.string().bind()
        val value = Arb.string().bind()
        val dataType = Arb.string().orNull().bind()
        SparqlItem(type, value, dataType)
    }

val sparqlUuidArb =
    arbitrary {
        SparqlItem(SparqlTypes.URI, "urn:uuid:${Arb.uuid().bind()}")
    }

val sparqlGraphArb =
    arbitrary {
        val graphs =
            listOf(
                Arb.constant("http://sv"),
                Arb.constant("http://ssh"),
                Arb.constant("http://tp"),
                Arb.constant("http://eq"),
            )
        SparqlItem(
            SparqlTypes.URI,
            Arb.choice(graphs).bind(),
        )
    }
val sparqlPredicateArb =
    arbitrary {
        val name = Arb.stringPattern("[a-z]+").bind()
        SparqlItem(
            "uri",
            "http://predicate.com#$name",
        )
    }

val sparqlObjectArb =
    arbitrary {
        val options =
            listOf(
                Arb.constant(Pair(SparqlTypes.URI, "urn:uuid:${Arb.uuid().bind()}")),
                Arb.constant(Pair(SparqlTypes.LITERAL, Arb.stringPattern("[a-zA-Z0-9-_]+").bind())),
                Arb.constant(Pair(SparqlTypes.LITERAL, Arb.float().bind().toString())),
            )
        val chosen = Arb.choice(options).bind()
        SparqlItem(chosen.first, chosen.second)
    }

class SmallCimModels(private val cim: String) {
    companion object {
        const val CON_NODE1_URI = "urn:uuid:12-af"
        const val CON_NODE2_URI = "urn:uuid:13-af"
    }

    fun twoTerminalsWithConnectivityNode(): SparqlResultJson {
        return SparqlResultJson(
            head = SparqlVars(listOf("graph", "s", "p", "o")),
            result =
                SparqlResult(
                    listOf(
                        mapOf(
                            "graph" to SparqlItem(SparqlTypes.URI, "http://g"),
                            "s" to SparqlItem(SparqlTypes.URI, "urn:uuid:12-ae"),
                            "p" to SparqlItem(SparqlTypes.URI, "$cim#Terminal.ConnectivityNode"),
                            "o" to SparqlItem(SparqlTypes.URI, CON_NODE1_URI),
                        ),
                        mapOf(
                            "graph" to SparqlItem(SparqlTypes.URI, "http://g"),
                            "s" to SparqlItem(SparqlTypes.URI, "urn:uuid:13-ae"),
                            "p" to SparqlItem(SparqlTypes.URI, "$cim#Terminal.ConnectivityNode"),
                            "o" to SparqlItem(SparqlTypes.URI, CON_NODE2_URI),
                        ),
                    ),
                ),
        )
    }

    fun twoConnectivityNodes(): SparqlResultJson {
        return SparqlResultJson(
            head = SparqlVars(listOf("graph", "s", "p", "o")),
            result =
                SparqlResult(
                    listOf(
                        mapOf(
                            "graph" to SparqlItem(SparqlTypes.URI, "http://g"),
                            "s" to SparqlItem(SparqlTypes.URI, CON_NODE1_URI),
                            "p" to SparqlItem(SparqlTypes.URI, "$cim#IdentifiedObject.name"),
                            "o" to SparqlItem(SparqlTypes.LITERAL, "Connectivity node 1"),
                        ),
                        mapOf(
                            "graph" to SparqlItem(SparqlTypes.URI, "http://g"),
                            "s" to SparqlItem(SparqlTypes.URI, CON_NODE2_URI),
                            "p" to SparqlItem(SparqlTypes.URI, "$cim#IdentifiedObject.name"),
                            "o" to SparqlItem(SparqlTypes.LITERAL, "Connectivity node 2"),
                        ),
                    ),
                ),
        )
    }
}
