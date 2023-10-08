package com.github.statnett.loadflowservice

import com.powsybl.contingency.ContingencyContext
import com.powsybl.contingency.ContingencyContextType
import com.powsybl.sensitivity.SensitivityFactor
import com.powsybl.sensitivity.SensitivityFunctionType
import com.powsybl.sensitivity.SensitivityVariableType
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.content.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private val logger = KotlinLogging.logger {}

@Serializable
class AutoSerializableSensitivityFactor(
    private val functionType: String,
    private val functionId: String,
    private val variableType: String,
    private val variableId: String,
    private val variableSet: Boolean,
    private val contingencyContextType: String
) {
    fun asSensitivityFactor(): SensitivityFactor {
        val ctgType = ContingencyContextType.valueOf(contingencyContextType)
        return SensitivityFactor(
            SensitivityFunctionType.valueOf(functionType), functionId,
            SensitivityVariableType.valueOf(variableType), variableId, variableSet,

            // TODO: Check how contingencyId is passed when type is SPECIAL
            ContingencyContext.create(null, ctgType)
        )
    }
}

typealias AutoSerializableSensitivityFactorList = List<AutoSerializableSensitivityFactor>

class SensitivityFactorContainer : FormItemLoadable {
    var factors: List<SensitivityFactor> = listOf()

    fun update(jsonString: String) {
        this.factors = Json.decodeFromString<AutoSerializableSensitivityFactorList>(jsonString).map { item ->
            item.asSensitivityFactor()
        }
    }

    override fun formItemHandler(part: PartData.FormItem) {
        val name = part.name ?: ""
        if (name == "sensitivity-factors") {
            this.update(part.value)
            logger.info { "Received sensitivity factors parameters: ${part.value}" }
        }
    }

}