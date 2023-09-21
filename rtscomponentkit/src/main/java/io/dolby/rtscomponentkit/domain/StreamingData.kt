package io.dolby.rtscomponentkit.domain

data class StreamingData(
    val accountId: String,
    val streamName: String,
    val useDevEnv: Boolean,
    val forcePlayOutDelay: Boolean,
    val disableAudio: Boolean,
    val rtcLogs: Boolean,
    val videoJitterMinimumDelayMs : Int
) {
    companion object {}
}
