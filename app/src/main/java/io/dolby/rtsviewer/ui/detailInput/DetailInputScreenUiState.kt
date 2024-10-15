package io.dolby.rtsviewer.ui.detailInput

import io.dolby.rtsviewer.datastore.StreamDetail

enum class RemoteConfigFetchState {
    IDLE, FETCHING, ERROR, SUCCESS
}

data class DetailInputScreenUiState(
    val recentStreams: List<StreamDetail> = emptyList(),
    val remoteConfigFetchState: RemoteConfigFetchState = RemoteConfigFetchState.IDLE
)
