package io.dolby.rtsviewer.ui.streaming

import com.millicast.AudioTrack
import com.millicast.VideoTrack

data class StreamingScreenUiState(
    val accountId: String = "",
    val streamName: String = "",
    val connecting: Boolean = true,
    val audioTrack: AudioTrack? = null,
    val videoTrack: VideoTrack? = null,
    val error: String? = null
)
