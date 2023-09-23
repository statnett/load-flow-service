package com.github.statnett.loadflowservice

import com.powsybl.iidm.network.Network
import com.powsybl.loadflow.LoadFlowParameters
import com.powsybl.loadflow.LoadFlow
import java.io.InputStream

fun networkFromStream(fname: String, content: InputStream): Network {
    return Network.read(fname, content)
} 

fun solve(network: Network, parameters: LoadFlowParameters) {
    LoadFlow.run(network, parameters);
}