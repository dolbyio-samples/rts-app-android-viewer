package io.dolby.rtsviewer.ui.streaming.stream

import android.graphics.SurfaceTexture
import com.millicast.devices.track.AudioTrack
import com.millicast.devices.track.VideoTrack
import com.millicast.subscribers.remote.RemoteAudioTrack
import com.millicast.subscribers.remote.RemoteVideoTrack
import io.dolby.rtsviewer.ui.streaming.common.AvailableStreamQuality
import io.dolby.rtsviewer.ui.streaming.common.StreamError
import io.dolby.rtsviewer.ui.streaming.container.StreamingContainerAction
import org.webrtc.VideoSink

// state
data class StreamState(
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
}
