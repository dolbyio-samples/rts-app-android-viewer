package io.dolby.rtsviewer.ui.streaming

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.dolby.rtscomponentkit.data.MultiStreamingData
import io.dolby.rtscomponentkit.data.MultiStreamingRepository
import io.dolby.rtscomponentkit.domain.StreamingData
import io.dolby.rtscomponentkit.utils.DispatcherProvider
import io.dolby.rtsviewer.datastore.RecentStreamsDataStore
import io.dolby.rtsviewer.ui.navigation.Screen
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class MultiStreamingViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val repository: MultiStreamingRepository,
    private val recentStreamsDataStore: RecentStreamsDataStore,
    private val dispatcherProvider: DispatcherProvider,
) : ViewModel() {
    private val _uiState = MutableStateFlow(MultiStreamingUiState())
    val uiState: StateFlow<MultiStreamingUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            connect()
            repository.data.collect { data ->
                update(data)
            }
        }
    }

    override fun onCleared() {
        repository.disconnect()
    }

    private fun getStreamName(handle: SavedStateHandle): String =
        handle[Screen.StreamingScreen.ARG_STREAM_NAME] ?: throw IllegalArgumentException()

    private fun getAccountId(handle: SavedStateHandle): String =
        handle[Screen.StreamingScreen.ARG_ACCOUNT_ID] ?: throw IllegalArgumentException()

    private suspend fun connect() {
        val streamName = getStreamName(savedStateHandle)

        val sd = recentStreamsDataStore.recentStream(streamName)
        val streamingData = if (sd != null) {
            StreamingData(
                streamName = streamName,
                accountId = sd.accountID,
                useDevEnv = sd.useDevEnv,
                disableAudio = sd.disableAudio,
                rtcLogs = sd.rtcLogs,
                videoJitterMinimumDelayMs = sd.videoJitterMinimumDelayMs
            )
        } else {
            StreamingData(
                streamName = streamName,
                accountId = getAccountId(savedStateHandle),
                useDevEnv = false,
                disableAudio = false,
                rtcLogs = false,
                videoJitterMinimumDelayMs = 0
            )
        }

        withContext(dispatcherProvider.main) {
            _uiState.update {
                it.copy(
                    inProgress = true,
                    accountId = streamingData.accountId,
                    streamName = streamingData.streamName
                )
            }
        }
        repository.connect(streamingData)
    }

    private suspend fun update(data: MultiStreamingData) = withContext(dispatcherProvider.main) {
        when {
            data.error != null -> _uiState.update { it.copy(error = data.error) }
            data.videoTracks.isNotEmpty() -> _uiState.update {
                it.copy(
                    inProgress = false,
                    videoTracks = data.videoTracks,
                    audioTracks = data.audioTracks
                )
            }
        }
    }
}