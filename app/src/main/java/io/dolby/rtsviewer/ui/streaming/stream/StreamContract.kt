package io.dolby.rtsviewer.ui.streaming.stream

import com.millicast.devices.track.AudioTrack
import com.millicast.devices.track.VideoTrack
import io.dolby.rtsviewer.ui.streaming.common.StreamError

// state
data class StreamState(
    val subscribed: Boolean = false,
    val disconnected: Boolean = false,
    val videoTrack: VideoTrack? = null,
    val audioTrack: AudioTrack? = null,
    val showStatistics: Boolean = false,
    val streamError: StreamError? = null
)

// ui state
data class StreamUiState(
    val subscribed: Boolean,
    val disconnected: Boolean,
    val videoTrack: VideoTrack?,
    val audioTrack: AudioTrack?,
    val showStatistics: Boolean,
    val streamError: StreamError?
)

// actions
enum class StreamAction {
    CONNECT,
    RELEASE
}
