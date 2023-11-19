package com.github.statnett.loadflowservice

import com.fasterxml.jackson.databind.ObjectMapper
import com.powsybl.security.SecurityAnalysisResult
import com.powsybl.security.json.SecurityAnalysisJsonModule
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object SecurityAnalysisResultSerializer : KSerializer<SecurityAnalysisResult> {
    private val mapper = ObjectMapper()

    init {
        mapper.registerModule(SecurityAnalysisJsonModule())
    }

    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("security-analysis-report", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: SecurityAnalysisResult) {
        val string = mapper.writeValueAsString(value)
        encoder.encodeString(string)
    }

    override fun deserialize(decoder: Decoder): SecurityAnalysisResult {
        val string = decoder.decodeString()
        return mapper.readValue(string, SecurityAnalysisResult::class.java)
    }
}