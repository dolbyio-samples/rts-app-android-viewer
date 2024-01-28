package io.dolby.interactiveplayer.rts.domain

import io.dolby.interactiveplayer.datastore.StreamDetail
import io.dolby.interactiveplayer.rts.data.VideoQuality

data class StreamingData(
    val accountId: String,
    val streamName: String
)

data class ConnectOptions(
    val useDevEnv: Boolean = false,
    val forcePlayOutDelay: Boolean = false,
    val disableAudio: Boolean = false,
    val rtcLogs: Boolean = false,
    val primaryVideoQuality: VideoQuality = VideoQuality.AUTO,
    val videoJitterMinimumDelayMs: Int = 20
) {
    companion object {
        fun from(streamDetail: StreamDetail): ConnectOptions =
            ConnectOptions(
                useDevEnv = streamDetail.useDevEnv,
                forcePlayOutDelay = streamDetail.forcePlayOutDelay,
                disableAudio = streamDetail.disableAudio,
                rtcLogs = streamDetail.rtcLogs,
                primaryVideoQuality = VideoQuality.valueToQuality(streamDetail.primaryVideoQuality),
                videoJitterMinimumDelayMs = streamDetail.videoJitterMinimumDelayMs
            )
    }
}
