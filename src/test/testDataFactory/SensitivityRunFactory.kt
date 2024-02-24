package testDataFactory

import com.github.statnett.loadflowservice.formItemHandlers.AutoSerializableSensitivityFactor
import io.ktor.client.request.forms.formData
import io.ktor.http.content.PartData
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class Contingencies(
    val type: String,
    val version: String,
    val name: String,
    val contingencies: List<Contingency>,
)

@Serializable
data class Contingency(
    val id: String,
    val elements: List<ContingencyElement>,
)

@Serializable
data class ContingencyElement(
    val id: String,
    val type: String,
)

fun ieee14BusContingencies(): Contingencies {
    val generator = ContingencyElement(id = "B3-G", type = "GENERATOR")
    val br1 = ContingencyElement(id = "L7-8-1", type = "BRANCH")
    val br2 = ContingencyElement(id = "L7-9-1", type = "BRANCH")

    return Contingencies(
        type = "default",
        version = "1.0",
        name = "list",
        contingencies =
            listOf(
                Contingency(id = "generatorContingency", listOf(generator)),
                Contingency(id = "branchContingency", listOf(br1, br2)),
            ),
    )
}

fun ieee14SensitivityFactor(): List<AutoSerializableSensitivityFactor> {
    return listOf(
        AutoSerializableSensitivityFactor(
            functionType = "BRANCH_ACTIVE_POWER_2",
            functionId = "L1-2-1",
            variableType = "INJECTION_ACTIVE_POWER",
            variableId = "B2-G",
            variableSet = false,
            contingencyContextType = "ALL",
        ),
    )
}

fun ieee14SensitivityLoadParams(): String {
    return "{\"dc\": true}"
}

fun ieee14SensitivityParams(): String {
    return "{\"voltage-voltage-sensitivity-value-threshold\": 0.001}"
}

data class SensitivityAnalysisConfig(
    val withContingencies: Boolean,
    val withLoadParameters: Boolean,
    val withSensitivityParameters: Boolean,
)

fun allSensitivityAnalysisConfigs(): List<SensitivityAnalysisConfig> {
    val options = listOf(true, false)
    return options.map { withCtg ->
        options.map { withLp ->
            options.map { withSensParam ->
                SensitivityAnalysisConfig(withCtg, withLp, withSensParam)
            }
        }.flatten()
    }.flatten()
}

fun loadParams(): List<PartData> {
    return formData {
        append(
            "load-parameters",
            ieee14SensitivityLoadParams(),
        )
    }
}

fun sensFactors(): List<PartData> {
    return formData {
        append(
            "sensitivity-factors",
            Json.encodeToString(ieee14SensitivityFactor()),
        )
    }
}

fun contingencies(): List<PartData> {
    return formData {
        append(
            "contingencies",
            Json.encodeToString(ieee14BusContingencies()),
        )
    }
}

fun sensParams(): List<PartData> {
    return formData {
        append(
            "sensitivity-analysis-parameters",
            ieee14SensitivityParams(),
        )
    }
}

data class SensitivityAnalysisFormDataContainer(
    val network: List<PartData> = formDataFromFile(ieeeCdfNetwork14CgmesFile()),
    val loadParams: List<PartData> = loadParams(),
    val sensFactors: List<PartData> = sensFactors(),
    val contingencies: List<PartData> = contingencies(),
    val sensParams: List<PartData> = sensParams(),
) {
    fun formData(config: SensitivityAnalysisConfig): List<PartData> {
        val parts: MutableList<PartData> = arrayListOf()
        parts += network
        parts += sensFactors

        if (config.withContingencies) {
            parts += contingencies
        }

        if (config.withSensitivityParameters) {
            parts += sensParams
        }

        if (config.withLoadParameters) {
            parts += loadParams
        }
        return parts
    }
}
