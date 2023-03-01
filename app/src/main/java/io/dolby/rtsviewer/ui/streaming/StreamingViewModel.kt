package io.dolby.rtsviewer.ui.streaming

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import io.dolby.rtscomponentkit.data.RTSViewerDataStore
import io.dolby.rtscomponentkit.utils.DispatcherProvider
import io.dolby.rtsviewer.ui.navigation.Screen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class StreamingViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    repository: RTSViewerDataStore,
    dispatcherProvider: DispatcherProvider
) : ViewModel() {
    private val defaultCoroutineScope = CoroutineScope(dispatcherProvider.default)
    private val _uiState = MutableStateFlow(StreamingScreenUiState())
    val uiState: StateFlow<StreamingScreenUiState> = _uiState.asStateFlow()

    init {
        defaultCoroutineScope.launch {
            val streamName = getStreamName(savedStateHandle)
            val accountId = getAccountId(savedStateHandle)
            withContext(dispatcherProvider.main) {
                _uiState.update { it.copy(accountId = accountId, streamName = streamName) }
            }
            repository.connect(streamName, accountId)
        }

        defaultCoroutineScope.launch {
            repository.state.collect {
                when (it) {
                    RTSViewerDataStore.State.Connected -> repository.startSubscribe()
                    RTSViewerDataStore.State.StreamInactive -> repository.stopSubscribe()
                    is RTSViewerDataStore.State.VideoTrackReady -> {
                        withContext(dispatcherProvider.main) {
                            _uiState.update { state ->
                                state.copy(
                                    connecting = false,
                                    videoTrack = it.videoTrack
                                )
                            }
                        }
                    }
                    is RTSViewerDataStore.State.Error -> {
                        withContext(dispatcherProvider.main) {
                            _uiState.update { state ->
                                state.copy(
                                    connecting = false,
                                    error = it.error.reason
                                )
                            }
                        }
                    }
                    else -> {}
                }
            }
        }
    }

    private fun getStreamName(handle: SavedStateHandle): String =
        handle[Screen.StreamingScreen.ARG_STREAM_NAME] ?: throw IllegalArgumentException()

    private fun getAccountId(handle: SavedStateHandle): String =
        handle[Screen.StreamingScreen.ARG_ACCOUNT_ID] ?: throw IllegalArgumentException()
}
