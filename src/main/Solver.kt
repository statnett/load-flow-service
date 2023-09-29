package com.github.statnett.loadflowservice

import com.powsybl.cgmes.conversion.CgmesImport
import com.powsybl.ieeecdf.converter.IeeeCdfImporter
import com.powsybl.iidm.network.ImportersLoader
import com.powsybl.iidm.network.ImportersLoaderList
import com.powsybl.iidm.network.Network
import com.powsybl.iidm.xml.XMLImporter
import com.powsybl.loadflow.LoadFlow
import com.powsybl.loadflow.LoadFlowParameters
import com.powsybl.loadflow.json.JsonLoadFlowParameters
import com.powsybl.matpower.converter.MatpowerImporter
import com.powsybl.powerfactory.converter.PowerFactoryImporter
import com.powsybl.psse.converter.PsseImporter
import com.powsybl.ucte.converter.UcteImporter
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream

fun networkFromStream(
    filename: String,
    content: InputStream,
): Network {
    val importerLoader = ImportersLoaderList(
        PsseImporter(), XMLImporter(), CgmesImport(),
        IeeeCdfImporter(), UcteImporter(),
        MatpowerImporter(), PowerFactoryImporter()
    )
    return Network.read(filename, content)
}

fun networkFromFileContent(content: FileContent): Network {
    return networkFromStream(content.name, ByteArrayInputStream(content.bytes))
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
        branches = branchPropertiesFromNetwork(network)
    )
}
