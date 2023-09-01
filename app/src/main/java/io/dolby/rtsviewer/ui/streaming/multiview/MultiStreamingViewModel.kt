package io.dolby.rtsviewer.ui.streaming.multiview

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
import io.dolby.rtsviewer.ui.streaming.Error
import io.dolby.rtsviewer.ui.streaming.StreamingViewModel.Companion.inboundRtpAudioVideoDataToList
import io.dolby.rtsviewer.utils.NetworkStatusObserver
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
    private val networkStatusObserver: NetworkStatusObserver
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
    }

    private fun getStreamName(handle: SavedStateHandle): String =
        handle[Screen.StreamingScreen.ARG_STREAM_NAME] ?: throw IllegalArgumentException()

    private fun getAccountId(handle: SavedStateHandle): String =
        handle[Screen.StreamingScreen.ARG_ACCOUNT_ID] ?: throw IllegalArgumentException()

    private suspend fun connect() {
        if (!repository.data.value.isSubscribed) {
            val streamName = getStreamName(savedStateHandle)

            val streamDetail = recentStreamsDataStore.recentStream(streamName)
            val streamingData = if (streamDetail != null) {
                StreamingData(
                    streamName = streamName,
                    accountId = streamDetail.accountID,
                    useDevEnv = streamDetail.useDevEnv,
                    disableAudio = streamDetail.disableAudio,
                    rtcLogs = streamDetail.rtcLogs,
                    videoJitterMinimumDelayMs = streamDetail.videoJitterMinimumDelayMs
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
    }

    private suspend fun update(data: MultiStreamingData) = withContext(dispatcherProvider.main) {
        when {
            data.error != null -> {
                networkStatusObserver.status.collect { networkStatus ->
                    when (networkStatus) {
                        NetworkStatusObserver.Status.Unavailable -> _uiState.update { it.copy(error = Error.NO_INTERNET_CONNECTION) }
                        else -> _uiState.update { it.copy(error = Error.STREAM_NOT_ACTIVE) }
                    }
                }
            }

            data.videoTracks.isNotEmpty() -> _uiState.update {
                it.copy(
                    inProgress = false,
                    videoTracks = data.videoTracks,
                    audioTracks = data.audioTracks,
                    selectedVideoTrackId = data.selectedVideoTrackId,
                    streamName = data.streamingData?.streamName,
                    statisticsData = data.statisticsData
                )
            }
        }
    }

    fun disconnect() {
        repository.disconnect()
    }

    fun selectVideoTrack(id: String?) {
        repository.updateSelectedVideoTrackId(id)
    }

    fun updateStatistics(show: Boolean) {
        _uiState.update { it.copy(showStatistics = show) }
    }

    fun streamingStatistics(mid: String?): List<Pair<Int, String>> {
        val selectedVideoTrackStatistics =
            uiState.value.statisticsData?.video?.firstOrNull { it.mid == mid }
        val selectedAudioTrackStatistics =
            uiState.value.statisticsData?.video?.firstOrNull()

        return inboundRtpAudioVideoDataToList(
            videoStatistics = selectedVideoTrackStatistics,
            audioStatistics = selectedAudioTrackStatistics
        )
    }
}
