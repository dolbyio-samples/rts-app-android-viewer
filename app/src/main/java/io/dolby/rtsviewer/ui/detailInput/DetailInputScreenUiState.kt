package io.dolby.rtsviewer.ui.detailInput

import io.dolby.rtsviewer.datastore.StreamDetail

data class DetailInputScreenUiState(
    val recentStreams: List<StreamDetail> = emptyList()
)
