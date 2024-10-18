package io.dolby.rtsviewer.ui.streaming.stream

import com.millicast.subscribers.remote.RemoteAudioTrack
import com.millicast.subscribers.remote.RemoteVideoTrack
import io.dolby.rtsviewer.ui.streaming.common.AvailableStreamQuality
import io.dolby.rtsviewer.ui.streaming.common.StreamError
import org.webrtc.VideoSink

// state
data class StreamState(
    val isFocused: Boolean = false,
    val connected: Boolean = false,
    val subscribed: Boolean = false,
    val disconnected: Boolean = false,
    val videoTrack: RemoteVideoTrack? = null,
    val audioTrack: RemoteAudioTrack? = null,
    val selectedStreamQuality: AvailableStreamQuality = AvailableStreamQuality.AUTO,
    val showStatistics: Boolean = false,
    val streamError: StreamError? = null
)

// ui state
data class StreamUiState(
    val shouldRequestFocusInitially: Boolean,
    val showSettingsButton: Boolean,
    val isFocused: Boolean,
    val subscribed: Boolean,
    val videoTrack: RemoteVideoTrack?,
    val selectedStreamQuality: AvailableStreamQuality,
    val showStatistics: Boolean,
    val streamError: StreamError?
)

// actions
sealed class StreamAction {
    object Connect : StreamAction()
    object Release : StreamAction()
    data class Play(val videoSink: VideoSink) : StreamAction()
    object Pause : StreamAction()
    data class UpdateFocus(val isFocused: Boolean) : StreamAction()
    data class UpdateSettingsVisibility(val show: Boolean) : StreamAction()
}
