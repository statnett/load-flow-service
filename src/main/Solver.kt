package com.github.statnett.loadflowservice

import com.fasterxml.jackson.databind.ObjectMapper
import com.powsybl.action.Action
import com.powsybl.commons.PowsyblException
import com.powsybl.commons.report.ReportNode
import com.powsybl.computation.local.LocalComputationManager
import com.powsybl.contingency.ContingenciesProvider
import com.powsybl.contingency.contingency.list.ContingencyList
import com.powsybl.iidm.network.ImportConfig
import com.powsybl.iidm.network.Importer
import com.powsybl.iidm.network.ImportersServiceLoader
import com.powsybl.iidm.network.Network
import com.powsybl.iidm.network.NetworkFactory
import com.powsybl.loadflow.LoadFlow
import com.powsybl.loadflow.LoadFlowParameters
import com.powsybl.loadflow.json.LoadFlowParametersJsonModule
import com.powsybl.security.LimitViolationFilter
import com.powsybl.security.SecurityAnalysis
import com.powsybl.security.SecurityAnalysisParameters
import com.powsybl.security.detectors.DefaultLimitViolationDetector
import com.powsybl.security.interceptors.SecurityAnalysisInterceptor
import com.powsybl.security.monitor.StateMonitor
import com.powsybl.security.strategy.OperatorStrategy
import com.powsybl.sensitivity.SensitivityAnalysis
import com.powsybl.sensitivity.SensitivityAnalysisParameters
import com.powsybl.sensitivity.SensitivityFactor
import com.powsybl.sensitivity.SensitivityVariableSet
import io.github.oshai.kotlinlogging.KotlinLogging
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
fun networkFromFileContent(content: NamedNetworkSource): Network {
    logger.info { "Loading network from file ${content.name}" }

    val importConfig = ImportConfig.CACHE.get()
    val loader = ImportersServiceLoader()
    val reporter = ReportNode.NO_OP
    val computationManager = LocalComputationManager.getDefault()
    val dataSource = content.asReadOnlyDataSource()
    val importer = Importer.find(dataSource, loader, computationManager, importConfig)
    if (importer != null) {
        logger.info { "Loading file using importer ${importer.javaClass}" }
        val networkFactory = NetworkFactory.findDefault()
        return importer.importData(dataSource, networkFactory, null, reporter)
    }
    throw PowsyblException("No importer found")
}

fun defaultLoadFlowParameters(): String {
    val parameters = LoadFlowParameters()
    val mapper = ObjectMapper()
    mapper.registerModule(LoadFlowParametersJsonModule())
    return mapper.writeValueAsString(parameters)
}

fun loadFlowTaskName(): String {
    return "load-flow"
}

fun loadFlowReporter(): ReportNode {
    val name = loadFlowTaskName()
    return ReportNode.newRootReportNode().withMessageTemplate(name, name).build()
}

fun reporterToString(reporter: ReportNode): String {
    val writer = StringWriter()

    // Check if we should use reporter writeJson instead
    reporter.print(writer)
    return writer.toString()
}

fun solve(
    network: Network,
    parameters: LoadFlowParameters,
): LoadFlowResultForApi {
    val reporter = loadFlowReporter()
    val result =
        LoadFlow.run(
            network,
            network.variantManager.workingVariantId,
            LocalComputationManager.getDefault(),
            parameters,
            reporter,
        )
    return LoadFlowResultForApi(
        isOk = !result.isFailed,
        buses = busPropertiesFromNetwork(network),
        branches = branchPropertiesFromNetwork(network),
        report = reporterToString(reporter),
    )
}

fun runSensitivityAnalysis(
    network: Network,
    factors: List<SensitivityFactor>,
    params: SensitivityAnalysisParameters,
    contingenciesList: ContingencyList,
): LoadFlowServiceSensitivityAnalysisResult {
    val reporter = ReportNode.newRootReportNode().withMessageTemplate("sensitivity", "").build()
    val variableSets: List<SensitivityVariableSet> = listOf()
    val contingencies = contingenciesList.getContingencies(network)

    val result =
        SensitivityAnalysis.run(
            network,
            network.variantManager.workingVariantId,
            factors,
            contingencies,
            variableSets,
            params,
            LocalComputationManager.getDefault(),
            reporter,
        )
    return LoadFlowServiceSensitivityAnalysisResult(
        result,
        reporterToString(reporter),
    )
}

fun runSecurityAnalysis(
    network: Network,
    params: SecurityAnalysisParameters,
    contingencies: ContingenciesProvider,
    intersceptors: List<SecurityAnalysisInterceptor>,
    operatorStrategies: List<OperatorStrategy>,
    actions: List<Action>,
    monitors: List<StateMonitor>,
): LoadFlowServiceSecurityAnalysisResult {
    val reporter = ReportNode.newRootReportNode().withMessageTemplate("security", "").build()
    val securityReport =
        SecurityAnalysis.run(
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
            reporter,
        )
    return LoadFlowServiceSecurityAnalysisResult(securityReport.result, reporterToString(reporter))
}
