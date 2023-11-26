package com.github.statnett.loadflowservice

import com.powsybl.iidm.network.Network
import com.powsybl.security.SecurityAnalysisResult
import com.powsybl.sensitivity.SensitivityAnalysisResult
import kotlinx.serialization.Serializable

/**
 * Class for holding properties from the PowsbyBl bus class that are
 * returned via the Rest API
 */
@Serializable
data class BusProperties(
    val id: String,
    val voltage: Double,
    val angle: Double,
    val activePower: Double,
    val reactivePower: Double,
)

fun busPropertiesFromNetwork(network: Network): List<BusProperties> {
    return network.busView.buses
        .map { bus ->
            BusProperties(
                id = bus.id,
                voltage = if (bus.v.isNaN()) bus.voltageLevel.nominalV else bus.v,
                angle = bus.angle,
                activePower = bus.p,
                reactivePower = bus.q,
            )
        }
        .toList()
}

@Serializable
data class LineProperties(
    val id: String,
    val isOverloaded: Boolean,
    val terminal1: TerminalProperties,
    val terminal2: TerminalProperties
)

@Serializable
data class TerminalProperties(val activePower: Double, val reactivePower: Double)

@Serializable
sealed class ComputationResult

@Serializable
data class LoadFlowResultForApi(
    val isOk: Boolean,
    val buses: List<BusProperties>,
    val branches: List<LineProperties>,
    val report: String
) : ComputationResult()

fun branchPropertiesFromNetwork(network: Network): List<LineProperties> {
    return network.lines.map { line ->
        LineProperties(
            id = line.id,
            isOverloaded = line.isOverloaded,
            terminal1 = TerminalProperties(line.terminal1.p, if (line.terminal1.q.isNaN()) 0.0 else line.terminal1.q),
            terminal2 = TerminalProperties(line.terminal2.p, if (line.terminal2.q.isNaN()) 0.0 else line.terminal2.q)
        )
    }.toList()
}

@Serializable
data class LoadFlowServiceSecurityAnalysisResult(
    @Serializable(with = SecurityAnalysisResultSerializer::class)
    val securityAnalysisResult: SecurityAnalysisResult,
    val report: String
) : ComputationResult()


@Serializable
data class LoadFlowServiceSensitivityAnalysisResult(
    @Serializable(with = SensitivityAnalysisResultSerializer::class)
    val sensitivityAnalysisResult: SensitivityAnalysisResult,
    val report: String
): ComputationResult()
