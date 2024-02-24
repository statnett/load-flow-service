package com.github.statnett.loadflowservice.formItemHandlers

import com.powsybl.sensitivity.SensitivityAnalysisParameters
import com.powsybl.sensitivity.json.JsonSensitivityAnalysisParameters
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.content.PartData

private val logger = KotlinLogging.logger {}

class SensitivityAnalysisParametersContainer : AutoVersionableJsonParser(), FormItemLoadable {
    var parameters = SensitivityAnalysisParameters()

    override fun currentVersion(): String {
        return SensitivityAnalysisParameters.VERSION
    }

    fun update(jsonString: String) {
        val withVersion = jsonStringWithVersion(jsonString)
        this.parameters = JsonSensitivityAnalysisParameters.update(this.parameters, withVersion.byteInputStream())
    }

    override fun formItemHandler(part: PartData.FormItem) {
        val name = part.name ?: ""
        if (name == "sensitivity-analysis-parameters") {
            this.update(part.value)
            logger.info { "Received load flow parameters: ${part.value}" }
        }
    }
}
