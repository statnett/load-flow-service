package testDataFactory

import com.github.statnett.loadflowservice.formItemHandlers.FormItemNames.Companion.SECURITY_ANALYSIS_PARAMS
import io.ktor.client.request.forms.formData
import io.ktor.http.content.PartData

fun ieee14SecurityParams(): String {
    return "{\"flow-proportional-threshold\": 0.2}"
}

fun securityParams(): List<PartData> {
    return formData {
        append(
            SECURITY_ANALYSIS_PARAMS,
            ieee14SecurityParams(),
        )
    }
}

data class SecurityAnalysisFormDataContainer(
    val network: List<PartData> = formDataFromFile(ieeeCdfNetwork14CgmesFile()),
    val loadParams: List<PartData> = loadParams(),
    val securityParams: List<PartData> = securityParams(),
    val contingencies: List<PartData> = contingencies(),
) {
    fun formData(): List<PartData> {
        val parts: MutableList<PartData> = arrayListOf()
        parts += network
        parts += contingencies
        // parts += securityParams
        parts += loadParams
        return parts
    }
}
