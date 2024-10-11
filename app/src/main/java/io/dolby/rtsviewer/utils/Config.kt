package io.dolby.rtsviewer.utils

import com.millicast.utils.LogLevel

data class StreamingConfig(val streams: List<StreamingConfigData>)

data class StreamingConfigData(
    val apiUrl: String, // TODO Remove
    val streamName: String,
    val accountId: String,
    val forcePlayoutDelayMin: Int? = null,
    val forcePlayoutDelayMax: Int? = null,
    val jitterBufferDelay: Int? = null,
    val forceSmooth: Boolean? = null,
    val logLevelWebRTC: LogLevel = LogLevel.MC_DEBUG,
    val logLevelSdk: LogLevel = LogLevel.MC_DEBUG,
    val logLevelWebSocket: LogLevel = LogLevel.MC_OFF
)
