package com.github.statnett.loadflowservice

import io.ktor.http.content.*

class MultiFormItemLoaders(private val loaders: List<FormItemLoadable>) : FormItemLoadable {

    override fun formItemHandler(part: PartData.FormItem) {
        this.loaders.forEach { loader -> loader.formItemHandler(part) }
    }
}