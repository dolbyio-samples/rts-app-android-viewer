package io.dolby.rtsviewer.utils

data class StreamingConfig(val streams: List<StreamingConfigData>)

data class StreamingConfigData(
    val apiUrl: String,
    val accountId: String,
    val streamName: String
)
