package io.dolby.rtsviewer.ui.savedStreams

import io.dolby.rtsviewer.datastore.StreamDetail

data class SavedStreamScreenUiState(
    val recentStreams: List<StreamDetail> = emptyList()
)
