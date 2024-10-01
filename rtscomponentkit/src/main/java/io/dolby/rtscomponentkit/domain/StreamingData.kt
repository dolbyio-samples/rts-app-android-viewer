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
    val serverEnv: ENV = ENV.PROD,
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
            val env = if (serverEnv.isEmpty()) {
                if (useDevEnv) {
                    ENV.DEV
                } else {
                    ENV.safeValueOf(serverEnv)
                }
            } else {
                ENV.safeValueOf(serverEnv)
            }

            return ConnectOptions(
                useDevEnv = useDevEnv,
                serverEnv = env,
                forcePlayOutDelay = forcePlayOutDelay,
                disableAudio = disableAudio,
                rtcLogs = rtcLogs,
                primaryVideoQuality = VideoQuality.valueToQuality(primaryVideoQuality),
                videoJitterMinimumDelayMs = videoJitterMinimumDelayMs
            )
        }
    }
}

enum class ENV {
    PROD, DEV, STAGE;

    companion object {
        fun safeValueOf(value: String): ENV {
            return try {
                ENV.valueOf(value)
            } catch (ex: Exception) {
                default
            }
        }

        val default: ENV = PROD

        fun listOfEnv() = listOf(PROD, DEV, STAGE)
    }

    fun getURL() = when (this) {
        PROD -> "https://director.millicast.com/api/director/subscribe"
        DEV -> "https://director-dev.millicast.com/api/director/subscribe"
        STAGE -> "https://director-staging.millicast.com/api/director/subscribe"
    }
}
