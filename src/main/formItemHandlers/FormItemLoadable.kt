package com.github.statnett.loadflowservice.formItemHandlers

import io.ktor.http.content.PartData

interface FormItemLoadable {
    fun formItemHandler(part: PartData.FormItem)
}
