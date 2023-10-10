package com.github.statnett.loadflowservice.formItemHandlers

import com.powsybl.contingency.contingency.list.ContingencyList
import com.powsybl.contingency.contingency.list.DefaultContingencyList
import com.powsybl.contingency.json.JsonContingencyListLoader
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.content.*

private val logger = KotlinLogging.logger {}

class ContingencyListContainer : AutoVersionableJsonParser(), FormItemLoadable {
    var contingencies: ContingencyList = DefaultContingencyList()

    override fun currentVersion(): String {
        return ContingencyList.VERSION
    }

    internal fun update(jsonString: String) {
        val withVersion = jsonStringWithVersion(jsonString)
        this.contingencies = JsonContingencyListLoader().load("contingencies.json", withVersion.byteInputStream())
    }

    override fun formItemHandler(part: PartData.FormItem) {
        val name = part.name ?: ""
        if (name == "contingencies") {
            this.update(part.value)
            logger.info { "Received contingencies: ${part.value}" }
        }
    }
}