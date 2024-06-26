package io.dolby.rtsviewer.ui.streaming

import com.millicast.subscribers.remote.RemoteAudioTrack
import com.millicast.subscribers.remote.RemoteVideoTrack
import io.dolby.rtscomponentkit.data.RTSViewerDataStore

enum class Error {
    NO_INTERNET_CONNECTION, STREAM_NOT_ACTIVE
}

data class StreamingScreenUiState(

    val accountId: String = "",
    val streamName: String = "",

    val connecting: Boolean = true,
    val disconnected: Boolean = false,
    val subscribed: Boolean = false,

    val audioTrack: RemoteAudioTrack? = null,
    val videoTrack: RemoteVideoTrack? = null,
    val viewerCount: Int = 0,
    val error: Error? = null,

    val streamQualityTypes: List<RTSViewerDataStore.StreamQualityType> = emptyList(),
    val selectedStreamQualityType: RTSViewerDataStore.StreamQualityType = RTSViewerDataStore.StreamQualityType.Auto,
    val pendingSelectedStreamQualityType: RTSViewerDataStore.StreamQualityType? = null
)
