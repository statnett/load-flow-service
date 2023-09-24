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
