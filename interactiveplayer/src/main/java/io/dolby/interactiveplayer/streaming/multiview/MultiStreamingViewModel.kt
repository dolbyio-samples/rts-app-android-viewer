package io.dolby.interactiveplayer.streaming.multiview

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.dolby.interactiveplayer.datastore.RecentStreamsDataStore
import io.dolby.interactiveplayer.navigation.Screen
import io.dolby.interactiveplayer.rts.data.MultiStreamingData
import io.dolby.interactiveplayer.rts.data.MultiStreamingRepository
import io.dolby.interactiveplayer.rts.domain.StatsInboundRtp.Companion.inboundRtpAudioVideoDataToList
import io.dolby.interactiveplayer.rts.domain.StreamingData
import io.dolby.interactiveplayer.rts.utils.DispatcherProvider
import io.dolby.interactiveplayer.utils.NetworkStatusObserver
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
    private val _statisticsState = MutableStateFlow(MultiStreamingStatisticsState())
    private val _videoQualityState = MutableStateFlow(MultiStreamingVideoQualityState())

    val uiState: StateFlow<MultiStreamingUiState> = _uiState.asStateFlow()
    val statisticsState: StateFlow<MultiStreamingStatisticsState> = _statisticsState.asStateFlow()
    val videoQualityState: StateFlow<MultiStreamingVideoQualityState> =
        _videoQualityState.asStateFlow()

    init {
        viewModelScope.launch {
            connect()
            repository.data.collect { data ->
                update(data)
                updateStatistics(data)
                updateLayers(data)
            }
        }
    }

    override fun onCleared() {
    }

    private fun getStreamName(handle: SavedStateHandle): String =
        handle[Screen.MultiStreamingScreen.ARG_STREAM_NAME] ?: throw IllegalArgumentException()

    private fun getAccountId(handle: SavedStateHandle): String =
        handle[Screen.MultiStreamingScreen.ARG_ACCOUNT_ID] ?: throw IllegalArgumentException()

    private suspend fun connect() {
        if (!repository.data.value.isSubscribed) {
            val streamName = getStreamName(savedStateHandle)

            val streamDetail = recentStreamsDataStore.recentStream(streamName)
            val streamingData = if (streamDetail != null) {
                StreamingData(
                    streamName = streamName.trim(),
                    accountId = streamDetail.accountID.trim()
                )
            } else {
                StreamingData(
                    streamName = streamName.trim(),
                    accountId = getAccountId(savedStateHandle)
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
                    layerData = data.trackLayerData
                )
            }
        }
    }

    private suspend fun updateStatistics(data: MultiStreamingData) =
        withContext(dispatcherProvider.main) {
            _statisticsState.update {
                it.copy(statisticsData = data.statisticsData)
            }
        }

    private suspend fun updateLayers(data: MultiStreamingData) =
        withContext(dispatcherProvider.main) {
            _videoQualityState.update {
                it.copy(
                    videoQualities = data.trackProjectedData.mapValues { it.value.videoQuality },
                    availableVideoQualities = data.trackLayerData
                )
            }
        }

    fun disconnect() {
        repository.disconnect()
    }

    fun selectVideoTrack(id: String?) {
        repository.updateSelectedVideoTrackId(id)
    }

    fun playVideo(
        video: MultiStreamingData.Video,
        preferredVideoQuality: MultiStreamingRepository.VideoQuality
    ) {
        repository.playVideo(
            video = video,
            preferredVideoQuality = preferredVideoQuality,
            preferredVideoQualities = _videoQualityState.value.preferredVideoQualities
        )
    }

    fun stopVideo(video: MultiStreamingData.Video) {
        repository.stopVideo(video)
    }

    fun updateStatistics(show: Boolean) {
        _statisticsState.update { it.copy(showStatistics = show) }
    }

    fun streamingStatistics(mid: String?): List<Pair<Int, String>> {
        val selectedVideoTrackStatistics =
            statisticsState.value.statisticsData?.video?.firstOrNull { it.mid == mid }
        val selectedAudioTrackStatistics =
            statisticsState.value.statisticsData?.audio?.firstOrNull()

        return inboundRtpAudioVideoDataToList(
            videoStatistics = selectedVideoTrackStatistics,
            audioStatistics = selectedAudioTrackStatistics
        )
    }

    fun showVideoQualitySelection(mid: String?, show: Boolean) {
        _videoQualityState.update { it.copy(showVideoQualitySelectionForMid = if (show) mid else null) }
    }

    fun preferredVideoQuality(mid: String?, videoQuality: MultiStreamingRepository.VideoQuality) {
        mid?.let {
            _videoQualityState.update {
                val currentPreferredVideoQuantities = it.preferredVideoQualities.toMutableMap()
                currentPreferredVideoQuantities[mid] = videoQuality
                it.copy(preferredVideoQualities = currentPreferredVideoQuantities)
            }
        }
    }
}
