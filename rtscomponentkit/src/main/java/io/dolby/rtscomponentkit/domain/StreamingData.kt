package io.dolby.rtscomponentkit.domain

import io.dolby.rtscomponentkit.data.RTSViewerDataStore.ENV
import io.dolby.rtscomponentkit.data.multistream.VideoQuality

data class StreamingData(
    val accountId: String,
    val streamName: String
) {
    companion object {
        const val DEMO_STREAM_NAME = "multiview"
        const val DEMO_ACCOUNT_ID = "k9Mwad"
    }
}

data class ConnectOptions(
    val useDevEnv: Boolean = false,
    val forcePlayOutDelay: Boolean = false,
    val disableAudio: Boolean = false,
    val rtcLogs: Boolean = false,
    val primaryVideoQuality: VideoQuality = VideoQuality.AUTO,
    val videoJitterMinimumDelayMs: Int = 20
) {
    companion object {
        fun from(
            useDevEnv: Boolean,
            forcePlayOutDelay: Boolean,
            disableAudio: Boolean,
            rtcLogs: Boolean,
            primaryVideoQuality: String,
            videoJitterMinimumDelayMs: Int
        ): ConnectOptions =
            ConnectOptions(
                useDevEnv = useDevEnv,
                forcePlayOutDelay = forcePlayOutDelay,
                disableAudio = disableAudio,
                rtcLogs = rtcLogs,
                primaryVideoQuality = VideoQuality.valueToQuality(primaryVideoQuality),
                videoJitterMinimumDelayMs = videoJitterMinimumDelayMs
            )
    }
}

fun listOfMediaServerEnv() = listOf(ENV.PROD, ENV.DEV, ENV.STAGE)
