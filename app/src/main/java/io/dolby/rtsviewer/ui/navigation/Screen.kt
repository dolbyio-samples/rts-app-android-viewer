package io.dolby.rtsviewer.ui.navigation

import io.dolby.rtscomponentkit.domain.StreamingData

sealed class Screen(val route: String) {
    object DetailInputScreen : Screen(route = "login") {
        const val ARG_STREAM_NAME_TO_PLAY = "streamNameToPlay"
        const val ARG_ACCOUNT_ID_TO_PLAY = "accountIDToPlay"
        const val ARG_USE_DEV_ENV = "useDevEnv"
        const val ARG_DISABLE_AUDIO = "disableAudio"
        const val ARG_RTC_LOGS = "rtcLogs"
        const val ARG_VIDEO_JITTER = "videoJitter"
    }

    object StreamingScreen :
        Screen(route = "streaming/streamName={streamName}&accountId={accountId}") {
        const val ARG_STREAM_NAME = "streamName"
        const val ARG_ACCOUNT_ID = "accountId"
        const val ARG_USE_DEV_ENV = "useDevEnv"
        const val ARG_DISABLE_AUDIO = "disableAudio"
        const val ARG_RTC_LOGS = "rtcLogs"
        const val ARG_VIDEO_JITTER = "videoJitter"
        fun route(model: StreamingData): String {
            val streamName = model.streamName
            val accountId = model.accountId
            return "streaming/streamName=$streamName&accountId=$accountId"
        }
    }

    object SavedStreams : Screen(route = "savedStreams")
}
