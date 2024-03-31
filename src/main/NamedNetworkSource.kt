package com.github.statnett.loadflowservice

import com.powsybl.commons.datasource.ReadOnlyDataSource

interface NamedNetworkSource {
    val name: String

    fun asReadOnlyDataSource(): ReadOnlyDataSource
}
