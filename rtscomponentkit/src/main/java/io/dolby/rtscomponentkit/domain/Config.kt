package io.dolby.rtscomponentkit.domain

import com.millicast.utils.LogLevel

// Follows JSON struct from remote config
data class RemoteStreamConfig(
    val forcePlayoutDelayMin: Int? = null,
    val forcePlayoutDelayMax: Int? = null,
    val jitterBufferDelay: Int? = null,
    val forceSmooth: Boolean? = null,
    val logLevelWebRTC: String? = null,
    val logLevelSdk: String? = null,
    val logLevelWebSocket: String? = null,
    val name: String,
    val desc: String,
    val urls: List<String>
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
    val name: String,
    val desc: String,
    val accountId: String,
    val streamName: String
) {
    companion object {
        fun from(streamConfig: RemoteStreamConfig, index: Int): StreamConfig {
            val details = parseUri(streamConfig.urls[index])
            return StreamConfig(
                forcePlayoutDelayMin = streamConfig.forcePlayoutDelayMin,
                forcePlayoutDelayMax = streamConfig.forcePlayoutDelayMax,
                jitterBufferDelay = streamConfig.jitterBufferDelay,
                forceSmooth = streamConfig.forceSmooth,
                logLevelWebRTC = toLogLevel(streamConfig.logLevelWebRTC),
                logLevelSdk = toLogLevel(streamConfig.logLevelSdk),
                logLevelWebSocket = toLogLevel(streamConfig.logLevelWebSocket),
                name = streamConfig.name,
                desc = streamConfig.desc,
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

        private fun toLogLevel(logLevel: String?): LogLevel {
            return when (logLevel?.lowercase()) {
                "verbose" -> LogLevel.MC_VERBOSE
                "debug" -> LogLevel.MC_DEBUG
                "log", "info" -> LogLevel.MC_LOG
                "warning" -> LogLevel.MC_WARNING
                "error" -> LogLevel.MC_ERROR
                "off" -> LogLevel.MC_OFF
                else -> LogLevel.MC_ERROR
            }
        }
    }
}
