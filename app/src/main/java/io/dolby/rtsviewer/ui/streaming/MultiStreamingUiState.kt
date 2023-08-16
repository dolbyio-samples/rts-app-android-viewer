package io.dolby.rtsviewer.ui.streaming

import io.dolby.rtscomponentkit.data.MultiStreamingData

data class MultiStreamingUiState(
    val inProgress: Boolean = false,
    val accountId: String? = null,
    val streamName: String? = null,
    val videoTracks: List<MultiStreamingData.Video> = emptyList(),
    val audioTracks: List<MultiStreamingData.Audio> = emptyList(),
    val error: String? = null
)
