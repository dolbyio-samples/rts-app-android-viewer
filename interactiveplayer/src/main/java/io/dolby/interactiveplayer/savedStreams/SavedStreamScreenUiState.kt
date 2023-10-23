package io.dolby.interactiveplayer.savedStreams

import io.dolby.interactiveplayer.datastore.StreamDetail

data class SavedStreamScreenUiState(
    val recentStreams: List<StreamDetail> = emptyList(),
    val loading: Boolean = true
)
