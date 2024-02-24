package com.github.statnett.loadflowservice.formItemHandlers

import com.powsybl.contingency.ContingenciesProvider
import com.powsybl.contingency.Contingency
import com.powsybl.contingency.contingency.list.ContingencyList
import com.powsybl.contingency.contingency.list.DefaultContingencyList
import com.powsybl.contingency.json.JsonContingencyListLoader
import com.powsybl.iidm.network.Network
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.content.PartData

private val logger = KotlinLogging.logger {}

class ContingencyListContainer : AutoVersionableJsonParser(), FormItemLoadable, ContingenciesProvider {
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

    override fun getContingencies(network: Network): List<Contingency> {
        return contingencies.getContingencies(network)
    }
}
