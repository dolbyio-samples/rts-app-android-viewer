package io.dolby.rtscomponentkit.domain

import com.millicast.utils.LogLevel

// Follows JSON struct from remote config
data class RemoteStreamConfig(
    val forcePlayoutDelayMin: Int? = null,
    val forcePlayoutDelayMax: Int? = null,
    val jitterBufferDelay: Int? = null,
    val forceSmooth: Boolean? = null,
    val logLevelWebRTC: LogLevel = LogLevel.MC_DEBUG,
    val logLevelSdk: LogLevel = LogLevel.MC_DEBUG,
    val logLevelWebSocket: LogLevel = LogLevel.MC_OFF,
    val multiStreamConfig: MultiStreamConfig
)

data class MultiStreamConfig(
    val name: String,
    val desc: String,
    val url: List<String>
)

// Parsed from JSON for consumptions
data class StreamConfigList(val streams: List<StreamConfig>)

data class StreamConfig(
    val forcePlayoutDelayMin: Int? = null,
    val forcePlayoutDelayMax: Int? = null,
    val jitterBufferDelay: Int? = null,
    val forceSmooth: Boolean? = null,
    val logLevelWebRTC: LogLevel = LogLevel.MC_DEBUG,
    val logLevelSdk: LogLevel = LogLevel.MC_DEBUG,
    val logLevelWebSocket: LogLevel = LogLevel.MC_OFF,
    val name: String? = null,
    val desc: String? = null,
    val accountId: String,
    val streamName: String
) {
    companion object {
        fun from(streamConfig: RemoteStreamConfig, index: Int): StreamConfig {
            val details = parseUri(streamConfig.multiStreamConfig.url[index])
            return StreamConfig(
                forcePlayoutDelayMin = streamConfig.forcePlayoutDelayMin,
                forcePlayoutDelayMax = streamConfig.forcePlayoutDelayMax,
                jitterBufferDelay = streamConfig.jitterBufferDelay,
                forceSmooth = streamConfig.forceSmooth,
                logLevelWebRTC = streamConfig.logLevelWebRTC,
                logLevelSdk = streamConfig.logLevelSdk,
                logLevelWebSocket = streamConfig.logLevelWebSocket,
                name = streamConfig.multiStreamConfig.name,
                desc = streamConfig.multiStreamConfig.desc,
                accountId = details?.first ?: "",
                streamName = details?.second ?: ""
            )
        }

        private fun parseUri(uri: String): Pair<String, String>? {
            val regex = Regex("dolbyio://([^/]+)/(.+)")
            val matchResult = regex.find(uri)

            return matchResult?.let {
                val (field1, field2) = it.destructured
                Pair(field1, field2)
            }
        }
    }
}
