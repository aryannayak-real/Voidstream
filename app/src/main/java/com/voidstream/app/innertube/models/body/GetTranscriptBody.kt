package com.voidstream.app.innertube.models.body

import com.voidstream.app.innertube.models.Context
import kotlinx.serialization.Serializable

@Serializable
data class GetTranscriptBody(
    val context: Context,
    val params: String,
)
