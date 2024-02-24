package com.github.statnett.loadflowservice.formItemHandlers

import com.github.statnett.loadflowservice.formItemHandlers.FormItemNames.Companion.LOAD_FLOW_PARAMS
import com.powsybl.loadflow.LoadFlowParameters
import com.powsybl.loadflow.json.JsonLoadFlowParameters
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.content.*

private val logger = KotlinLogging.logger {}

/**
 * Convenience class used to deserialize and update a load parameter instance
 */
class LoadParameterContainer : AutoVersionableJsonParser(), FormItemLoadable {
    var parameters = LoadFlowParameters()
    private var parametersModified = false

    override fun currentVersion(): String {
        return LoadFlowParameters.VERSION
    }

    private fun update(jsonString: String) {
        val withVersion = jsonStringWithVersion(jsonString)
        this.parameters = JsonLoadFlowParameters.update(this.parameters, withVersion.byteInputStream())
        this.parametersModified = true
    }

    override fun formItemHandler(part: PartData.FormItem) {
        val name = part.name ?: ""
        if (name == LOAD_FLOW_PARAMS) {
            this.update(part.value)
            logger.info { "Received load flow parameters: ${part.value}" }
        }
    }
}