package io.dolby.rtsviewer.ui.navigation

import io.dolby.rtscomponentkit.domain.StreamingData

sealed class Screen(val route: String) {
    object DetailInputScreen : Screen(route = "login") {
        const val ARG_STREAM_NAME_TO_PLAY = "streamNameToPlay"
        const val ARG_ACCOUNT_ID_TO_PLAY = "accountIDToPlay"
    }

    object StreamingContainerScreen : Screen(route = "streamingContainer")

    object SavedStreams : Screen(route = "savedStreams")
}
