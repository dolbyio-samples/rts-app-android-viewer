package io.dolby.rtsviewer.ui.streaming

import com.millicast.AudioTrack
import com.millicast.VideoTrack

data class Error(val title: String, val subtitle: String?)

data class StreamingScreenUiState(

    val accountId: String = "",
    val streamName: String = "",

    val connecting: Boolean = true,
    val disconnected: Boolean = false,
    val subscribed: Boolean = false,

    val audioTrack: AudioTrack? = null,
    val videoTrack: VideoTrack? = null,
    val viewerCount: Int = 0,
    val error: Error? = null,

    val showLiveIndicator: Boolean = false
)
