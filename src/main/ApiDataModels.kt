package com.github.statnett.loadflowservice

import com.powsybl.iidm.network.Network
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
                voltage = bus.v,
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
data class LoadFlowResultForApi(val isOk: Boolean, val buses: List<BusProperties>, val branches: List<LineProperties>)

fun branchPropertiesFromNetwork(network: Network): List<LineProperties> {
    return network.lines.map { line ->
        LineProperties(
            id = line.id,
            isOverloaded = line.isOverloaded,
            terminal1 = TerminalProperties(line.terminal1.p, line.terminal1.q),
            terminal2 = TerminalProperties(line.terminal2.p, line.terminal2.q)
        )
    }.toList()
}