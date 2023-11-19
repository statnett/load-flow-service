package com.github.statnett.loadflowservice.formItemHandlers

import com.powsybl.security.SecurityAnalysisParameters
import com.powsybl.security.json.JsonSecurityAnalysisParameters
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.content.*

private val logger = KotlinLogging.logger {}

class SecurityAnalysisParametersContainer : AutoVersionableJsonParser(), FormItemLoadable {
    var parameters = SecurityAnalysisParameters()

    override fun currentVersion(): String {
        return SecurityAnalysisParameters.VERSION
    }

    fun update(jsonString: String) {
        val withVersion = jsonStringWithVersion(jsonString)
        this.parameters = JsonSecurityAnalysisParameters.update(this.parameters, withVersion.byteInputStream())
    }

    override fun formItemHandler(part: PartData.FormItem) {
        val name = part.name ?: ""
        if (name == "security-analysis-parameters") {
            this.update(part.value)
            logger.info { "Received security analysis parameters: ${part.value}" }
        }
    }
}
