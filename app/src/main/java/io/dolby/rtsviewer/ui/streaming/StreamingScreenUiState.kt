package io.dolby.rtsviewer.ui.streaming

import com.millicast.AudioTrack
import com.millicast.VideoTrack
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

    val audioTrack: AudioTrack? = null,
    val videoTrack: VideoTrack? = null,
    val viewerCount: Int = 0,
    val error: Error? = null,

    val showLiveIndicator: Boolean = false,

    val streamQualityTypes: List<RTSViewerDataStore.StreamQualityType> = emptyList(),
    val selectedStreamQualityType: RTSViewerDataStore.StreamQualityType = RTSViewerDataStore.StreamQualityType.Auto
)
