package com.github.statnett.loadflowservice

import com.powsybl.iidm.network.ImportersServiceLoader
import com.powsybl.iidm.network.Network
import com.powsybl.loadflow.LoadFlow
import com.powsybl.loadflow.LoadFlowParameters
import com.powsybl.loadflow.json.JsonLoadFlowParameters
import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.ByteArrayOutputStream
import java.io.InputStream

private val logger = KotlinLogging.logger {}

fun networkFromStream(
    filename: String,
    content: InputStream,
): Network {
    warnOnFewAvailableImporters()
    return Network.read(filename, content)
}

fun warnOnFewAvailableImporters() {
    val numLoaders = ImportersServiceLoader().loadImporters().size
    val expect = 7

    // TODO: Currently this happens when the executable jar is run with the java command
    // does not happen when run via mvn exec:java.
    // Too few loaders will most likely result errors when trying load many file formats
    if (numLoaders < expect) {
        logger.warn { "Few available loadImporters ($numLoaders)" }
    }
}

fun networkFromFileContent(content: FileContent): Network {
    logger.info { "Loading network from file ${content.name}" }
    return networkFromStream(content.name, content.contentAsStream())
}

fun defaultLoadFlowParameters(): String {
    val parameters = LoadFlowParameters()
    val stream = ByteArrayOutputStream()
    JsonLoadFlowParameters.write(parameters, stream)

    // TODO: here we undo pretty printing. See if a raw JSON string can be obtained in a more elegant way
    return undoPrettyPrintJson(stream.toString())
}

fun solve(
    network: Network,
    parameters: LoadFlowParameters,
): LoadFlowResultForApi {
    val result = LoadFlow.run(network, parameters)
    return LoadFlowResultForApi(
        isOk = result.isOk,
        buses = busPropertiesFromNetwork(network),
        branches = branchPropertiesFromNetwork(network),
        log = result.logs ?: ""
    )
}
