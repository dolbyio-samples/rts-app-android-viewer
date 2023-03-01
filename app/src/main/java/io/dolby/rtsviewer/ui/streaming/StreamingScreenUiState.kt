package io.dolby.rtsviewer.ui.streaming

import com.millicast.VideoTrack

data class StreamingScreenUiState(
    val accountId: String = "",
    val streamName: String = "",
    val connecting: Boolean = true,
    val videoTrack: VideoTrack? = null,
    val error: String? = null
)
