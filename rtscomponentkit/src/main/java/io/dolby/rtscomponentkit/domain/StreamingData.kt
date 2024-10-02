package io.dolby.rtscomponentkit.domain

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
    val mediaServerEnv: MediaServerEnv = MediaServerEnv.PROD,
    val forcePlayOutDelay: Boolean = false,
    val disableAudio: Boolean = false,
    val rtcLogs: Boolean = false,
    val primaryVideoQuality: VideoQuality = VideoQuality.AUTO,
    val videoJitterMinimumDelayMs: Int = 20
) {
    companion object {
        fun from(
            useDevEnv: Boolean,
            serverEnv: String,
            forcePlayOutDelay: Boolean,
            disableAudio: Boolean,
            rtcLogs: Boolean,
            primaryVideoQuality: String,
            videoJitterMinimumDelayMs: Int
        ): ConnectOptions {
            val mediaServerEnv = if (serverEnv.isEmpty()) {
                if (useDevEnv) {
                    MediaServerEnv.DEV
                } else {
                    MediaServerEnv.safeValueOf(serverEnv)
                }
            } else {
                MediaServerEnv.safeValueOf(serverEnv)
            }
            return ConnectOptions(
                useDevEnv = useDevEnv,
                mediaServerEnv = mediaServerEnv,
                forcePlayOutDelay = forcePlayOutDelay,
                disableAudio = disableAudio,
                rtcLogs = rtcLogs,
                primaryVideoQuality = VideoQuality.valueToQuality(primaryVideoQuality),
                videoJitterMinimumDelayMs = videoJitterMinimumDelayMs
            )
        }
    }
}

enum class MediaServerEnv {
    PROD, DEV, STAGE;
    companion object {
        fun safeValueOf(value: String): MediaServerEnv {
            return try {
                MediaServerEnv.valueOf(value)
            } catch (ex: Exception) {
                default
            }
        }
        val default: MediaServerEnv = PROD
        fun listOfEnv() = MediaServerEnv.values()
    }
    fun getURL() = when (this) {
        PROD -> "https://director.millicast.com/api/director/subscribe"
        DEV -> "https://director-dev.millicast.com/api/director/subscribe"
        STAGE -> "https://director-staging.millicast.com/api/director/subscribe"
    }
}
