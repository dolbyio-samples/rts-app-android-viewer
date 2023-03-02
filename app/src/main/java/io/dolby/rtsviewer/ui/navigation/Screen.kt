package io.dolby.rtsviewer.ui.navigation

import io.dolby.rtscomponentkit.domain.StreamingData

sealed class Screen(val route: String) {
    object DetailInputScreen : Screen(route = "login")
    object StreamingScreen :
        Screen(route = "streaming/streamName={streamName}&accountId={accountId}") {
        const val ARG_STREAM_NAME = "streamName"
        const val ARG_ACCOUNT_ID = "accountId"
        fun route(model: StreamingData): String {
            val streamName = model.streamName
            val accountId = model.accountId
            return "streaming/streamName=$streamName&accountId=$accountId"
        }
    }
}