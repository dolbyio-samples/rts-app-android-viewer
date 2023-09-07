package io.dolby.interactiveplayer.detailInput

import io.dolby.interactiveplayer.datastore.StreamDetail

data class DetailInputScreenUiState(
    val recentStreams: List<StreamDetail> = emptyList()
)
