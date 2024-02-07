package io.dolby.interactiveplayer.streaming.multiview

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.dolby.interactiveplayer.datastore.RecentStreamsDataStore
import io.dolby.interactiveplayer.navigation.Screen
import io.dolby.interactiveplayer.preferenceStore.MultiviewLayout
import io.dolby.interactiveplayer.preferenceStore.PrefsStore
import io.dolby.interactiveplayer.preferenceStore.StreamSortOrder
import io.dolby.interactiveplayer.rts.data.MultiStreamingRepository
import io.dolby.interactiveplayer.rts.data.VideoQuality
import io.dolby.interactiveplayer.rts.data.safeLaunch
import io.dolby.interactiveplayer.rts.domain.ConnectOptions
import io.dolby.interactiveplayer.rts.domain.MultiStreamingData
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
    private val networkStatusObserver: NetworkStatusObserver,
    private val prefsStore: PrefsStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(MultiStreamingUiState())
    private val _statisticsState = MutableStateFlow(MultiStreamingStatisticsState())
    private val _videoQualityState = MutableStateFlow(MultiStreamingVideoQualityState())
    private val _multiviewLayout = MutableStateFlow(MultiviewLayout.default)
    private val _showSourceLabels = MutableStateFlow(true)
    private val _streamSortOrder = MutableStateFlow(StreamSortOrder.default)

    val uiState: StateFlow<MultiStreamingUiState> = _uiState.asStateFlow()
    val statisticsState: StateFlow<MultiStreamingStatisticsState> = _statisticsState.asStateFlow()
    val videoQualityState: StateFlow<MultiStreamingVideoQualityState> =
        _videoQualityState.asStateFlow()
    val multiviewLayout = _multiviewLayout.asStateFlow()
    val showSourceLabels = _showSourceLabels.asStateFlow()

    init {
        viewModelScope.safeLaunch({
            repository.data.collect { data ->
                update(data)
                updateStatistics(data)
                updateLayers(data)
            }
        })
        viewModelScope.safeLaunch({
            connect()
        })
        viewModelScope.launch {
            networkStatusObserver.status.collect { networkStatus ->
                when (networkStatus) {
                    NetworkStatusObserver.Status.Unavailable -> _uiState.update { it.copy(hasNetwork = false) }
                    else -> _uiState.update { it.copy(hasNetwork = true) }
                }
            }
        }
        viewModelScope.launch {
            prefsStore.multiviewLayout(streamingData()).collect { layout ->
                _multiviewLayout.update { layout }
            }
        }
        viewModelScope.launch {
            prefsStore.showSourceLabels(streamingData()).collect { show ->
                _showSourceLabels.update { show }
            }
        }
        viewModelScope.launch {
            prefsStore.streamSourceOrder(streamingData()).collect { sortOrder ->
                _streamSortOrder.update { sortOrder }
            }
        }
    }

    override fun onCleared() {
    }

    private fun getStreamName(handle: SavedStateHandle): String =
        handle[Screen.MultiStreamingScreen.ARG_STREAM_NAME] ?: throw IllegalArgumentException()

    private fun getAccountId(handle: SavedStateHandle): String =
        handle[Screen.MultiStreamingScreen.ARG_ACCOUNT_ID] ?: throw IllegalArgumentException()

    private fun streamingData(): StreamingData =
        StreamingData(getAccountId(savedStateHandle), getStreamName(savedStateHandle))

    private suspend fun connect() {
        val streamName = getStreamName(savedStateHandle)

        val streamDetail = recentStreamsDataStore.recentStream(
            accountId = getAccountId(savedStateHandle),
            streamName = streamName
        )
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
        val connectOptions =
            streamDetail?.let { ConnectOptions.from(streamDetail) } ?: ConnectOptions()
        withContext(dispatcherProvider.main) {
            _uiState.update {
                it.copy(
                    accountId = streamingData.accountId,
                    streamName = streamingData.streamName,
                    connectOptions = connectOptions
                )
            }
        }

        if (!repository.data.value.isSubscribed) {
            _uiState.update {
                it.copy(
                    inProgress = true
                )
            }
            repository.connect(streamingData, connectOptions)
        }
    }

    private suspend fun update(data: MultiStreamingData) = withContext(dispatcherProvider.main) {
        val videoTracks = data.videoTracks
        val alphaNumericComparator = Comparator<MultiStreamingData.Video> { source1, source2 ->
            when {
                source1.sourceId == null -> 1
                source2.sourceId == null -> -1
                else -> source1.sourceId.compareTo(source2.sourceId)
            }
        }
        when {
            data.error != null || videoTracks.isEmpty() -> {
                _uiState.update {
                    val error = if (it.hasNetwork) {
                        Error.STREAM_NOT_ACTIVE
                    } else {
                        Error.NO_INTERNET_CONNECTION
                    }
                    it.copy(error = error)
                }
            }

            else -> _uiState.update {
                it.copy(
                    inProgress = false,
                    videoTracks = if (_streamSortOrder.value == StreamSortOrder.AlphaNumeric) {
                        videoTracks.sortedWith(alphaNumericComparator)
                    } else videoTracks,
                    audioTracks = data.audioTracks,
                    selectedVideoTrackId = data.selectedVideoTrackId,
                    streamName = data.streamingData?.streamName,
                    layerData = data.trackLayerData,
                    error = null
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

    fun disconnect() = viewModelScope.safeLaunch({ repository.disconnect() })

    fun selectVideoTrack(sourceId: String?) {
        repository.updateSelectedVideoTrackId(sourceId)
    }

    fun playVideo(
        video: MultiStreamingData.Video,
        preferredVideoQuality: VideoQuality
    ) = viewModelScope.safeLaunch({
        repository.playVideo(
            video = video,
            preferredVideoQuality = preferredVideoQuality,
            preferredVideoQualities = _videoQualityState.value.preferredVideoQualities
        )
    })

    fun stopVideo(video: MultiStreamingData.Video) = viewModelScope.safeLaunch({
        repository.stopVideo(video)
    })

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

    fun preferredVideoQuality(mid: String?, videoQuality: VideoQuality) {
        mid?.let {
            _videoQualityState.update {
                val currentPreferredVideoQuantities = it.preferredVideoQualities.toMutableMap()
                currentPreferredVideoQuantities[mid] = videoQuality
                it.copy(preferredVideoQualities = currentPreferredVideoQuantities)
            }
        }
    }
}
