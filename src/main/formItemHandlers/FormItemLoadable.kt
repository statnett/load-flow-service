package com.github.statnett.loadflowservice.formItemHandlers

import io.ktor.http.content.*

interface FormItemLoadable {
    fun formItemHandler(part: PartData.FormItem)
}