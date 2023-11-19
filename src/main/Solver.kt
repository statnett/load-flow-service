package com.github.statnett.loadflowservice

import com.fasterxml.jackson.databind.ObjectMapper
import com.powsybl.commons.PowsyblException
import com.powsybl.commons.json.JsonUtil
import com.powsybl.commons.reporter.Reporter
import com.powsybl.commons.reporter.ReporterModel
import com.powsybl.computation.local.LocalComputationManager
import com.powsybl.contingency.ContingenciesProvider
import com.powsybl.contingency.contingency.list.ContingencyList
import com.powsybl.iidm.network.*
import com.powsybl.loadflow.LoadFlow
import com.powsybl.loadflow.LoadFlowParameters
import com.powsybl.loadflow.json.JsonLoadFlowParameters
import com.powsybl.security.LimitViolationFilter
import com.powsybl.security.SecurityAnalysis
import com.powsybl.security.SecurityAnalysisParameters
import com.powsybl.security.SecurityAnalysisReport
import com.powsybl.security.SecurityAnalysisResult
import com.powsybl.security.action.Action
import com.powsybl.security.detectors.DefaultLimitViolationDetector
import com.powsybl.security.interceptors.SecurityAnalysisInterceptor
import com.powsybl.security.json.SecurityAnalysisJsonModule
import com.powsybl.security.monitor.StateMonitor
import com.powsybl.security.strategy.OperatorStrategy
import com.powsybl.sensitivity.*
import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.ByteArrayOutputStream
import java.io.StringWriter


private val logger = KotlinLogging.logger {}


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

class NoFileProvidedException(message: String) : Exception(message)

fun networkFromFirstFile(files: List<FileContent>): Network {
    if (files.isEmpty()) {
        throw NoFileProvidedException("No file with model data provided")
    }
    return networkFromFileContent(files[0])
}

// This function follows closely the functionality implemented in Powsybl-core Network.read
// However, here we create the ReadOnlyMemDataSource our self which supports constructing it
// from a zip archive (e.g. CIM/XML files zipped).
fun networkFromFileContent(content: FileContent): Network {
    logger.info { "Loading network from file ${content.name}" }

    val importConfig = ImportConfig.CACHE.get()
    val loader = ImportersServiceLoader()
    val reporter = Reporter.NO_OP
    val computationManager = LocalComputationManager.getDefault()
    val dataSource = content.asReadOnlyMemDataSource()
    val importer = Importer.find(dataSource, loader, computationManager, importConfig)
    if (importer != null) {
        val networkFactory = NetworkFactory.findDefault()
        return importer.importData(dataSource, networkFactory, null, reporter)
    }
    throw PowsyblException("No importer found")
}

fun defaultLoadFlowParameters(): String {
    val parameters = LoadFlowParameters()
    val stream = ByteArrayOutputStream()
    JsonLoadFlowParameters.write(parameters, stream)

    // TODO: here we undo pretty printing. See if a raw JSON string can be obtained in a more elegant way
    return undoPrettyPrintJson(stream.toString())
}

fun loadFlowTaskName(): String {
    return "load-flow"
}

fun loadFlowReporter(): ReporterModel {
    val name = loadFlowTaskName()
    return ReporterModel(name, name)
}

fun reporterToString(reporter: ReporterModel): String {
    val writer = StringWriter()
    reporter.export(writer)
    return writer.toString()
}

fun solve(
    network: Network,
    parameters: LoadFlowParameters,
): LoadFlowResultForApi {
    val reporter = loadFlowReporter()
    val result = LoadFlow.run(
        network,
        network.variantManager.workingVariantId,
        LocalComputationManager.getDefault(),
        parameters,
        reporter
    )
    return LoadFlowResultForApi(
        isOk = result.isOk,
        buses = busPropertiesFromNetwork(network),
        branches = branchPropertiesFromNetwork(network),
        report = reporterToString(reporter)
    )
}

fun runSensitivityAnalysis(
    network: Network,
    factors: List<SensitivityFactor>,
    params: SensitivityAnalysisParameters,
    contingenciesList: ContingencyList
): String {
    val reporter = ReporterModel("sensitivity", "")
    val variableSets: List<SensitivityVariableSet> = listOf()
    val contingencies = contingenciesList.getContingencies(network)
    val factorReader = SensitivityFactorModelReader(factors, network)

    val factory = JsonUtil.createJsonFactory()
    val writer = StringWriter()
    val jsonGenerator = factory.createGenerator(writer)
    jsonGenerator.writeStartObject()
    jsonGenerator.writeFieldName("sensitivity-results")
    val resultWriter = SensitivityResultJsonWriter(jsonGenerator, contingencies)

    SensitivityAnalysis.run(
        network,
        network.variantManager.workingVariantId,
        factorReader,
        resultWriter,
        contingencies,
        variableSets,
        params,
        LocalComputationManager.getDefault(),
        reporter
    )
    // Close the nested array created by Powsybl
    jsonGenerator.writeEndArray()
    jsonGenerator.writeEndArray()

    // Add run report to the JSON
    jsonGenerator.writeStringField("report", reporterToString(reporter))
    jsonGenerator.writeEndObject()
    jsonGenerator.close()
    return writer.toString()
}

fun runSecurityAnalysis(
    network: Network,
    params: SecurityAnalysisParameters,
    contingencies: ContingenciesProvider,
    intersceptors: List<SecurityAnalysisInterceptor>,
    operatorStrategies: List<OperatorStrategy>,
    actions: List<Action>,
    monitors: List<StateMonitor>
): LoadFlowServiceSecurityAnalysisResult {
    val reporter = ReporterModel("security", "")
    val securityReport = SecurityAnalysis.run(
        network,
        network.variantManager.workingVariantId,
        contingencies,
        params,
        LocalComputationManager.getDefault(),
        LimitViolationFilter.load(),
        DefaultLimitViolationDetector(),
        intersceptors,
        operatorStrategies,
        actions,
        monitors,
        reporter
    )
    return LoadFlowServiceSecurityAnalysisResult(securityReport.result, reporterToString(reporter))
}