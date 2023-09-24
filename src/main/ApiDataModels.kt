package com.github.statnett.loadflowservice

import com.powsybl.iidm.network.Network

/**
 * Class for holding properties from the PowsbyBl bus class that are
 * returned via the Rest API
 */
data class BusProperties(
    val id: String,
    val voltage: Double,
    val angle: Double,
    val activePower: Double,
    val reactivePower: Double,
)

fun busPropertiesFromNetwork(network: Network) =
    network.getBusView().getBusStream().map {
        BusProperties(
            id = it.getId(),
            voltage = it.getV(),
            angle = it.getAngle(),
            activePower = it.getP(),
            reactivePower = it.getQ(),
        )
    }
