package io.dolby.rtsviewer.ui.streaming

import android.icu.text.SimpleDateFormat
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.millicast.video.TextureViewRenderer
import dagger.hilt.android.lifecycle.HiltViewModel
import io.dolby.rtscomponentkit.R
import io.dolby.rtscomponentkit.data.RTSViewerDataStore
import io.dolby.rtscomponentkit.domain.MultiStreamStatisticsData
import io.dolby.rtscomponentkit.domain.StatsInboundRtp.Companion.msNormalised
import io.dolby.rtscomponentkit.utils.DispatcherProvider
import io.dolby.rtsviewer.preferenceStore.PrefsStore
import io.dolby.rtsviewer.utils.NetworkStatusObserver
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.CharacterIterator
import java.text.StringCharacterIterator
import java.util.Date
import javax.inject.Inject

private const val TAG = "StreamingViewModel"
private const val SHOW_TOOLBAR_TIMEOUT: Long = 5_000

@HiltViewModel
class StreamingViewModel @Inject constructor(
    private val repository: RTSViewerDataStore,
    private val dispatcherProvider: DispatcherProvider,
    private val preferencesDataStore: PrefsStore,
    private val networkStatusObserver: NetworkStatusObserver
) : ViewModel() {
    private val _uiState = MutableStateFlow(StreamingScreenUiState())
    val uiState: StateFlow<StreamingScreenUiState> = _uiState.asStateFlow()

    private val _showLiveIndicator = MutableStateFlow(false)
    val showLiveIndicator = _showLiveIndicator.asStateFlow()

    private val _showToolbarState = MutableStateFlow(false)
    val showToolbarState = _showToolbarState.asStateFlow()
    private val _showToolbarDelayState = MutableStateFlow(0L)

    private val _showStatistics = MutableStateFlow(false)
    val showStatistics = _showStatistics.asStateFlow()

    private val _showSettings = MutableStateFlow(false)
    val showSettings = _showSettings.asStateFlow()

    val streamingStatistics: Flow<List<Pair<Int, String>>?> = streamingStatistics()

    private val _showSimulcastSettings = MutableStateFlow(false)
    var showSimulcastSettings = _showSimulcastSettings.asStateFlow()

    private var alreadyCleared = false

    init {
        viewModelScope.launch {
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
                                if (_uiState.value.audioTrack == null || _uiState.value.audioTrack?.isActive == false) {
                                    Log.d(TAG, "AudioTrackReady")
                                    withContext(dispatcherProvider.main) {
                                        _uiState.update { state ->
                                            state.copy(
                                                audioTrack = dataStoreState.audioTrack
                                            )
                                        }
                                    }
                                }
                            }

                            is RTSViewerDataStore.State.VideoTrackReady -> {
                                if (_uiState.value.videoTrack == null) {
                                    Log.d(TAG, "VideoTrackReady")
                                    withContext(dispatcherProvider.main) {
                                        _uiState.update { state ->
                                            state.copy(
                                                videoTrack = dataStoreState.videoTrack
                                            )
                                        }
                                    }
                                }
                            }

                            RTSViewerDataStore.State.Disconnecting,
                            RTSViewerDataStore.State.Disconnected -> {
                                Log.d(
                                    TAG,
                                    if (dataStoreState == RTSViewerDataStore.State.Disconnecting) {
                                        "Disconnecting"
                                    } else "Disconnected"
                                )
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

        viewModelScope.launch {
            preferencesDataStore.isLiveIndicatorEnabled.collect { enabled ->
                _showLiveIndicator.update { enabled }
            }
        }

        viewModelScope.launch {
            repository.streamQualityTypes
                .collectLatest {
                    withContext(dispatcherProvider.main) {
                        _uiState.update { state ->
                            state.copy(
                                streamQualityTypes = it[state.videoTrack?.currentMid] ?: emptyList()
                            )
                        }
                    }
                }
        }

        viewModelScope.launch {
            repository.selectedStreamQualityType
                .collectLatest {
                    withContext(dispatcherProvider.main) {
                        _uiState.update { state ->
                            state.copy(
                                selectedStreamQualityType = it
                            )
                        }
                    }
                }
        }
    }

    override fun onCleared() {
        clear()
    }

    fun clear() {
        if (!alreadyCleared) {
            alreadyCleared = true
            repository.disconnect()
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

    fun updateStatistics(state: Boolean) {
        _showStatistics.update { state }
    }

    fun updateShowLiveIndicator(show: Boolean) {
        viewModelScope.launch {
            preferencesDataStore.updateLiveIndicator(show)
        }
    }

    fun settingsVisibility(visible: Boolean) {
        _showSettings.update { visible }
    }

    private fun streamingStatistics(): Flow<List<Pair<Int, String>>?> =
        repository.statisticsData.map { statisticsData -> getStatisticsValuesList(statisticsData) }

    private fun getStatisticsValuesList(statisticsData: MultiStreamStatisticsData?): List<Pair<Int, String>>? {
        val currentVideoMid = _uiState.value.videoTrack?.currentMid
        val currentAudioMid = _uiState.value.audioTrack?.currentMid
        statisticsData?.let { statistics ->
            val statisticsValuesList = mutableListOf<Pair<Int, String>>()
            statistics.roundTripTime?.let {
                statisticsValuesList.add(
                    Pair(
                        R.string.statisticsScreen_rtt,
                        "${it.times(1000).toLong()} ms"
                    )
                )
            }
            statistics.availableOutgoingBitrate?.let {
                statisticsValuesList.add(
                    Pair(
                        R.string.statisticsScreen_outgoingBitrate,
                        "${it.div(1000)} kbps"
                    )
                )
            }
            val currentVideo = statistics.video?.firstOrNull { it.mid == currentVideoMid }
            val currentAudio = statistics.audio?.firstOrNull { it.mid == currentAudioMid }
            currentVideo?.videoResolution?.let {
                statisticsValuesList.add(Pair(R.string.statisticsScreen_videoResolution, it))
            }
            _uiState.value.selectedStreamQualityType.layerData?.original?.targetBitrate?.let {
                statisticsValuesList.add(
                    Pair(
                        io.dolby.rtsviewer.R.string.statisticsScreen_targetBitrate,
                        "${it.div(1000)} kbps"
                    )
                )
            }
            currentVideo?.fps?.let {
                statisticsValuesList.add(Pair(R.string.statisticsScreen_fps, "${it.toLong()}"))
            }

            currentVideo?.processingDelay?.let { pd ->
                currentVideo.framesDecoded?.let { fd ->
                    statisticsValuesList.add(
                        Pair(
                            R.string.statisticsScreen_processing_delay,
                            String.format("%.2f ms", msNormalised(pd, fd.toDouble()))
                        )
                    )
                }
            }

            currentVideo?.jitterBufferDelay?.let { jbd ->
                currentVideo.jitterBufferEmittedCount.let { jbec ->
                    statisticsValuesList.add(
                        Pair(
                            R.string.statisticsScreen_video_jitter_bufffer_target_delay,
                            String.format("%.2f ms", msNormalised(jbd, jbec.toDouble()))
                        )
                    )
                }
            }

            currentVideo?.jitterBufferMinimumDelay?.let { jbmd ->
                currentVideo.jitterBufferEmittedCount.let { jbec ->
                    statisticsValuesList.add(
                        Pair(
                            R.string.statisticsScreen_jitter_bufffer_min_delay,
                            String.format("%.2f ms", msNormalised(jbmd, jbec.toDouble()))
                        )
                    )
                }
            }

            currentVideo?.packetsLost?.let {
                statisticsValuesList.add(Pair(R.string.statisticsScreen_videoLoss, "$it"))
            }
            currentAudio?.packetsLost?.let {
                statisticsValuesList.add(Pair(R.string.statisticsScreen_audioLoss, "$it"))
            }

            currentVideo?.framesDropped?.let {
                statisticsValuesList.add(Pair(R.string.statisticsScreen_video_packets_dropped, "$it"))
            }

            currentVideo?.jitter?.let {
                statisticsValuesList.add(
                    Pair(
                        R.string.statisticsScreen_videoJitter,
                        "${it.times(1000)} ms"
                    )
                )
            }
            currentAudio?.jitter?.let {
                statisticsValuesList.add(
                    Pair(
                        R.string.statisticsScreen_audioJitter,
                        "${it.times(1000)} ms"
                    )
                )
            }
            var codecNames = ""
            currentVideo?.codecName?.let {
                codecNames += it
            }
            currentAudio?.codecName?.let {
                if (codecNames.isNotEmpty()) codecNames += ", "
                codecNames += it
            }
            if (codecNames.isNotEmpty()) {
                statisticsValuesList.add(Pair(R.string.statisticsScreen_codecs, codecNames))
            }
            statistics.timestamp?.let { doubleTime ->
                getDateTime(doubleTime.toLong())?.let { dateTime ->
                    statisticsValuesList.add(Pair(R.string.statisticsScreen_timestamp, dateTime))
                }
            }
            return statisticsValuesList
        }
        return null
    }

    private fun getDateTime(timeStamp: Long): String? {
        return try {
            val dateFormat = SimpleDateFormat.getDateTimeInstance()
            val netDate = Date(timeStamp)
            dateFormat.format(netDate)
        } catch (e: Exception) {
            Log.e(TAG, e.toString())
            null
        }
    }

    private fun formattedByteCount(bytes: Long): String {
        var value = bytes
        if (-1000 < value && value < 1000) {
            return "$value B"
        }
        val ci: CharacterIterator = StringCharacterIterator("kMGTPE")
        while (value <= -999950 || value >= 999950) {
            value /= 1000
            ci.next()
        }
        return String.format("%.1f %cB", value / 1000.0, ci.current())
    }

    fun updateShowSimulcastSettings(show: Boolean) {
        _showSimulcastSettings.update { show }
    }

    fun selectStreamQualityType(streamQualityType: RTSViewerDataStore.StreamQualityType) {
        _uiState.update { state ->
            state.copy(pendingSelectedStreamQualityType = streamQualityType)
        }
    }

    fun playVideo(
        view: TextureViewRenderer,
        preferredQuality: RTSViewerDataStore.StreamQualityType? = null
    ) {
        var streamQualityType: RTSViewerDataStore.StreamQualityType? =
            _uiState.value.pendingSelectedStreamQualityType ?: _uiState.value.selectedStreamQualityType
        streamQualityType?.let { qualityType ->
            _uiState.update {
                it.copy(
                    selectedStreamQualityType = qualityType
                )
            }
        } ?: { streamQualityType = _uiState.value.selectedStreamQualityType }
        val qualityToApply = (streamQualityType ?: preferredQuality ?: RTSViewerDataStore.StreamQualityType.Auto).layerData
        Log.d(TAG, "playVideo $qualityToApply $streamQualityType $preferredQuality")
        _uiState.value.videoTrack?.enableAsync(
            promote = true,
            layer = qualityToApply,
            videoSink = view
        )
        _uiState.value.audioTrack?.enableAsync()
    }
    fun pauseVideo() {
        _uiState.value.videoTrack?.disableAsync()
        _uiState.value.audioTrack?.disableAsync()
    }
}
