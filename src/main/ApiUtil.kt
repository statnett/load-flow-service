package com.github.statnett.loadflowservice

import java.io.ByteArrayInputStream

fun busesFromRequest(
    type: String,
    body: ByteArray,
): List<BusProperties> {
    val network = networkFromStream(type, ByteArrayInputStream(body))
    return busPropertiesFromNetwork(network)
}
