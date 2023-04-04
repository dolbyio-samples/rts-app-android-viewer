package io.dolby.rtsviewer.ui.streaming

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.dolby.rtscomponentkit.data.RTSViewerDataStore
import io.dolby.rtscomponentkit.utils.DispatcherProvider
import io.dolby.rtsviewer.preferenceStore.PrefsStore
import io.dolby.rtsviewer.ui.navigation.Screen
import io.dolby.rtsviewer.utils.NetworkStatusObserver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

private const val TAG = "StreamingViewModel"
private const val SHOW_TOOLBAR_TIMEOUT: Long = 5_000

@HiltViewModel
class StreamingViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val repository: RTSViewerDataStore,
    private val dispatcherProvider: DispatcherProvider,
    private val preferencesDataStore: PrefsStore,
    private val networkStatusObserver: NetworkStatusObserver
) : ViewModel() {
    private val defaultCoroutineScope = CoroutineScope(dispatcherProvider.default)
    private val _uiState = MutableStateFlow(StreamingScreenUiState())
    val uiState: StateFlow<StreamingScreenUiState> = _uiState.asStateFlow()
    private val _showToolbarState = MutableStateFlow(false)
    private val _showToolbarDelayState = MutableStateFlow(0L)
    var showToolbarState = _showToolbarState.asStateFlow()

    init {
        defaultCoroutineScope.launch {
            tickerFlow(5.seconds)
                .onEach {
                    if (!_uiState.value.connecting && (_uiState.value.error != null || _uiState.value.disconnected)) {
                        Log.d(TAG, "Reconnect")
                        connect()
                    }
                }
                .launchIn(viewModelScope)
        }

        defaultCoroutineScope.launch {
            repository.state.combine(networkStatusObserver.status) { f1, f2 -> Pair(f1, f2) }
                .collect { (dataStoreState, networkStatus) ->
                    when (networkStatus) {
                        NetworkStatusObserver.Status.Unavailable -> withContext(dispatcherProvider.main) {
                            Log.d(TAG, "Internet connection error")
                            _uiState.update { state ->
                                state.copy(
                                    error = Error.NO_INTERNET_CONNECTION
                                )
                            }
                        }
                        NetworkStatusObserver.Status.Available -> when (dataStoreState) {
                            RTSViewerDataStore.State.Connecting -> {
                                Log.d(TAG, "Connecting")
                                withContext(dispatcherProvider.main) {
                                    _uiState.update { state ->
                                        state.copy(
                                            connecting = true
                                        )
                                    }
                                }
                            }
                            RTSViewerDataStore.State.Subscribed -> {
                                Log.d(TAG, "Subscribed")
                                repository.audioPlaybackStart()
                                withContext(dispatcherProvider.main) {
                                    _uiState.update { state ->
                                        state.copy(
                                            subscribed = true,
                                            connecting = false,
                                            error = null,
                                            disconnected = false
                                        )
                                    }
                                }
                            }
                            RTSViewerDataStore.State.StreamActive -> {
                                Log.d(TAG, "StreamActive")
                                withContext(dispatcherProvider.main) {
                                    _uiState.update { state ->
                                        state.copy(
                                            subscribed = true,
                                            connecting = false,
                                            error = null,
                                            disconnected = false
                                        )
                                    }
                                }
                            }
                            RTSViewerDataStore.State.StreamInactive -> {
                                Log.d(TAG, "StreamInactive")
                                repository.stopSubscribe()
                                withContext(dispatcherProvider.main) {
                                    _uiState.update { state ->
                                        state.copy(
                                            subscribed = false,
                                            disconnected = true
                                        )
                                    }
                                }
                            }
                            is RTSViewerDataStore.State.AudioTrackReady -> {
                                Log.d(TAG, "AudioTrackReady")
                                withContext(dispatcherProvider.main) {
                                    _uiState.update { state ->
                                        state.copy(
                                            audioTrack = dataStoreState.audioTrack
                                        )
                                    }
                                }
                            }
                            is RTSViewerDataStore.State.VideoTrackReady -> {
                                Log.d(TAG, "VideoTrackReady")
                                withContext(dispatcherProvider.main) {
                                    _uiState.update { state ->
                                        state.copy(
                                            videoTrack = dataStoreState.videoTrack
                                        )
                                    }
                                }
                            }
                            RTSViewerDataStore.State.Disconnected -> {
                                Log.d(TAG, "Disconnected")
                                withContext(dispatcherProvider.main) {
                                    _uiState.update { state ->
                                        state.copy(
                                            connecting = false,
                                            disconnected = true
                                        )
                                    }
                                }
                            }
                            is RTSViewerDataStore.State.Error -> {
                                Log.d(TAG, "Error")
                                withContext(dispatcherProvider.main) {
                                    _uiState.update { state ->
                                        state.copy(
                                            connecting = false,
                                            error = Error.STREAM_NOT_ACTIVE
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
        }

        defaultCoroutineScope.launch {
            preferencesDataStore.isLiveIndicatorEnabled
                .collectLatest {
                    withContext(dispatcherProvider.main) {
                        _uiState.update { state ->
                            state.copy(
                                showLiveIndicator = it
                            )
                        }
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

    fun showToolbar() {
        _showToolbarDelayState.update { _showToolbarDelayState.value + 1 }
        if (!_showToolbarState.value) {
            viewModelScope.launch {
                _showToolbarState.update { true }
                while (_showToolbarDelayState.value > 0) {
                    delay(SHOW_TOOLBAR_TIMEOUT)
                    _showToolbarDelayState.update { _showToolbarDelayState.value - 1 }
                }
                _showToolbarState.update { false }
            }
        }
    }

    fun hideToolbar() {
        _showToolbarState.update { false }
    }

    fun updateShowLiveIndicator(show: Boolean) {
        defaultCoroutineScope.launch {
            preferencesDataStore.updateLiveIndicator(show)
        }
    }
}
