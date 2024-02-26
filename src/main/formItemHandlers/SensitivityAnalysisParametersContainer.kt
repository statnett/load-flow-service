package com.github.statnett.loadflowservice.formItemHandlers

import com.github.statnett.loadflowservice.formItemHandlers.FormItemNames.Companion.SENSITIVITY_ANALYSIS_PARAMS
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
        if (name == SENSITIVITY_ANALYSIS_PARAMS) {
            this.update(part.value)
            logger.info { "Received load flow parameters: ${part.value}" }
        }
    }
}
