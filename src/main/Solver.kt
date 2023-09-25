package com.github.statnett.loadflowservice

import com.powsybl.iidm.network.Network
import com.powsybl.loadflow.LoadFlow
import com.powsybl.loadflow.LoadFlowParameters
import com.powsybl.loadflow.json.JsonLoadFlowParameters
import java.io.ByteArrayOutputStream
import java.io.InputStream

fun networkFromStream(
    fname: String,
    content: InputStream,
): Network {
    return Network.read(fname, content)
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
) {
    LoadFlow.run(network, parameters)
}


