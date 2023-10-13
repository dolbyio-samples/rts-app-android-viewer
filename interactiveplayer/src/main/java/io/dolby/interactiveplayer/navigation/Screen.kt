package io.dolby.interactiveplayer.navigation

import io.dolby.interactiveplayer.rts.domain.StreamingData

sealed class Screen(val route: String) {
    object DetailInputScreen : Screen(route = "login") {
        const val ARG_STREAM_NAME_TO_PLAY = "streamNameToPlay"
        const val ARG_ACCOUNT_ID_TO_PLAY = "accountIDToPlay"
        const val ARG_USE_DEV_ENV = "useDevEnv"
        const val ARG_DISABLE_AUDIO = "disableAudio"
        const val ARG_RTC_LOGS = "rtcLogs"
        const val ARG_VIDEO_JITTER = "videoJitter"
    }

    object MultiStreamingScreen :
        Screen(route = "multistreaming/streamName={streamName}&accountId={accountId}") {
        const val ARG_STREAM_NAME = "streamName"
        const val ARG_ACCOUNT_ID = "accountId"
        fun route(model: StreamingData): String {
            val streamName = model.streamName
            val accountId = model.accountId
            return "multistreaming/streamName=$streamName&accountId=$accountId"
        }
    }

    object SingleStreamingScreen : Screen(route = "single/streamName={streamName}&accountId={accountId}") {
        fun route(model: StreamingData): String {
            val streamName = model.streamName
            val accountId = model.accountId
            return "single/streamName=$streamName&accountId=$accountId"
        }
    }

    object SavedStreams : Screen(route = "savedStreams")

    object RecentStreams : Screen(route = "recentStreams")
    object GlobalSettings : Screen(route = "globalSettings")
    object StreamSettings :
        Screen(route = "streamSettings/streamName={streamName}&accountId={accountId}") {
        const val ARG_STREAM_NAME = "streamName"
        const val ARG_ACCOUNT_ID = "accountId"
        fun route(model: StreamingData?): String {
            return model?.streamName?.let { streamName ->
                "streamSettings/streamName=$streamName&accountId=${model.accountId}"
            } ?: "globalSettings"
        }
    }
}
