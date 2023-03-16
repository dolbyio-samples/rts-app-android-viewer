package io.dolby.rtsviewer.ui.streaming

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.dolby.rtscomponentkit.data.RTSViewerDataStore
import io.dolby.rtscomponentkit.utils.DispatcherProvider
import io.dolby.rtsviewer.ui.navigation.Screen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@HiltViewModel
class StreamingViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val repository: RTSViewerDataStore,
    private val dispatcherProvider: DispatcherProvider
) : ViewModel() {
    private val defaultCoroutineScope = CoroutineScope(dispatcherProvider.default)
    private val _uiState = MutableStateFlow(StreamingScreenUiState())
    val uiState: StateFlow<StreamingScreenUiState> = _uiState.asStateFlow()

    init {
        defaultCoroutineScope.launch {
            tickerFlow(5.seconds)
                .onEach {
                    if (_uiState.value.error != null || _uiState.value.disconnected) {
                        connect()
                    }
                }
                .launchIn(viewModelScope)
        }

        defaultCoroutineScope.launch {
            repository.state.collect {
                when (it) {
                    RTSViewerDataStore.State.Connected -> {
                        repository.startSubscribe()
                    }
                    RTSViewerDataStore.State.Subscribed -> {
                        repository.audioPlaybackStart()
                    }
                    RTSViewerDataStore.State.StreamInactive -> {
                        repository.stopSubscribe()
                    }
                    is RTSViewerDataStore.State.VideoTrackReady -> {
                        withContext(dispatcherProvider.main) {
                            _uiState.update { state ->
                                state.copy(
                                    connecting = false,
                                    videoTrack = it.videoTrack,
                                    error = null
                                )
                            }
                        }
                    }
                    RTSViewerDataStore.State.StreamInactive -> {
                    }
                    RTSViewerDataStore.State.Disconnected -> {
                        withContext(dispatcherProvider.main) {
                            _uiState.update { state ->
                                state.copy(
                                    disconnected = true
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

    override fun onCleared() {
        repository.stopSubscribe()
    }

    private suspend fun connect() {
        val streamName = getStreamName(savedStateHandle)
        val accountId = getAccountId(savedStateHandle)
        withContext(dispatcherProvider.main) {
            _uiState.update { it.copy(accountId = accountId, streamName = streamName) }
        }
        repository.connect(streamName, accountId)
    }

    private fun getStreamName(handle: SavedStateHandle): String =
        handle[Screen.StreamingScreen.ARG_STREAM_NAME] ?: throw IllegalArgumentException()

    private fun getAccountId(handle: SavedStateHandle): String =
        handle[Screen.StreamingScreen.ARG_ACCOUNT_ID] ?: throw IllegalArgumentException()

    private fun tickerFlow(period: Duration, initialDelay: Duration = Duration.ZERO) = flow {
        delay(initialDelay)
        while (true) {
            emit(Unit)
            delay(period)
        }
    }
}
