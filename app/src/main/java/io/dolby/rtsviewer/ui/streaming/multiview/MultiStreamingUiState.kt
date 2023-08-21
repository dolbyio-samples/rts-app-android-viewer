package io.dolby.rtsviewer.ui.streaming.multiview

import io.dolby.rtscomponentkit.data.MultiStreamingData

data class MultiStreamingUiState(
    val inProgress: Boolean = false,
    val accountId: String? = null,
    val streamName: String? = null,
    val videoTracks: List<MultiStreamingData.Video> = emptyList(),
    val audioTracks: List<MultiStreamingData.Audio> = emptyList(),
    val selectedVideoTrackId: String? = null,
    val error: String? = null
)
