package io.dolby.rtsviewer.ui.streaming

import android.icu.text.SimpleDateFormat
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.dolby.rtscomponentkit.data.RTSViewerDataStore
import io.dolby.rtscomponentkit.data.StatisticsData
import io.dolby.rtscomponentkit.domain.StreamingData
import io.dolby.rtscomponentkit.utils.DispatcherProvider
import io.dolby.rtsviewer.R
import io.dolby.rtsviewer.datastore.RecentStreamsDataStore
import io.dolby.rtsviewer.preferenceStore.PrefsStore
import io.dolby.rtsviewer.ui.navigation.Screen
import io.dolby.rtsviewer.utils.NetworkStatusObserver
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.CharacterIterator
import java.text.StringCharacterIterator
import java.util.Date
import javax.inject.Inject
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

private const val TAG = "StreamingViewModel"
private const val SHOW_TOOLBAR_TIMEOUT: Long = 5_000

@HiltViewModel
class StreamingViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val repository: RTSViewerDataStore,
    private val recentStreamsDataStore: RecentStreamsDataStore,
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

    init {
        viewModelScope.launch {
            tickerFlow(5.seconds).onEach {
                Log.d(">>>>>>", _uiState.value.toString())
                if (_uiState.value.initiateConnection || (!_uiState.value.connecting && (_uiState.value.error != null || _uiState.value.disconnected))) {
                    // TODO: Figure out why this is firing again before state is changed to connecting
                    _uiState.update {
                        it.copy(initiateConnection = false)
                    }
                    connect()
                }
            }.launchIn(viewModelScope)
        }

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
                                            initiateConnection = false,
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
                                streamQualityTypes = it
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
        repository.stopSubscribe()
    }

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
                    accountId = streamingData.accountId,
                    streamName = streamingData.streamName
                )
            }
        }
        repository.connect(streamingData)
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

    private fun getStatisticsValuesList(statisticsData: StatisticsData?): List<Pair<Int, String>>? {
        statisticsData?.let { statistics ->
            val statisticsValuesList = mutableListOf<Pair<Int, String>>()

            statistics.video?.mid?.let {
                statisticsValuesList.add(Pair(R.string.statisticsScreen_mid, it))
            }

            statistics.video?.decoderImplementation?.let {
                statisticsValuesList.add(Pair(R.string.statisticsScreen_decoder_impl, it))
            }

            statistics.video?.processingDelay?.let { pd ->
                statistics.video?.framesDecoded?.let { fd ->
                    statisticsValuesList.add(
                        Pair(
                            R.string.statisticsScreen_processing_delay,
                            String.format("%.2f ms", msNormalised(pd, fd.toDouble()))
                        )
                    )
                }
            }

            statistics.video?.decodeTime?.let { dt ->
                statistics.video?.framesDecoded?.let { fd ->
                    statisticsValuesList.add(
                        Pair(
                            R.string.statisticsScreen_decode_time,
                            String.format("%.2f ms", msNormalised(dt, fd.toDouble()))
                        )
                    )
                }
            }

            statistics.video?.videoResolution?.let {
                statisticsValuesList.add(Pair(R.string.statisticsScreen_videoResolution, it))
            }

            statistics.video?.fps?.let {
                statisticsValuesList.add(Pair(R.string.statisticsScreen_fps, "${it.toLong()}"))
            }

            statistics.video?.bytesReceived?.let {
                statisticsValuesList.add(
                    Pair(
                        R.string.statisticsScreen_videoTotal,
                        formattedByteCount(it.toLong())
                    )
                )
            }

            statistics.video?.packetsReceived?.let {
                statisticsValuesList.add(Pair(R.string.statisticsScreen_packets_received, "$it"))
            }

            statistics.video?.framesDecoded?.let {
                statisticsValuesList.add(Pair(R.string.statisticsScreen_frames_decoded, "$it"))
            }

            statistics.video?.framesDropped?.let {
                statisticsValuesList.add(Pair(R.string.statisticsScreen_packets_dropped, "$it"))
            }

            statistics.video?.jitterBufferEmittedCount?.let {
                statisticsValuesList.add(Pair(R.string.statisticsScreen_jitter_bufffer_ec, "$it"))
            }

            statistics.video?.jitter?.let {
                statisticsValuesList.add(
                    Pair(
                        R.string.statisticsScreen_videoJitter,
                        "${it.times(1000)} ms"
                    )
                )
            }

            statistics.video?.jitterBufferDelay?.let { jbd ->
                statistics.video?.jitterBufferEmittedCount?.let { jbec ->
                    statisticsValuesList.add(
                        Pair(
                            R.string.statisticsScreen_jitter_bufffer_delay,
                            String.format("%.2f ms", msNormalised(jbd, jbec.toDouble()))
                        )
                    )
                }
            }

            statistics.video?.jitterBufferDelay?.let { jbd ->
                statistics.video?.jitterBufferEmittedCount?.let { jbec ->
                    statisticsValuesList.add(
                        Pair(
                            R.string.statisticsScreen_jitter_bufffer_target_delay,
                            String.format("%.2f ms", msNormalised(jbd, jbec.toDouble()))
                        )
                    )
                }
            }

//            statistics.video?.jitterBufferTargetDelay?.let {
//                statisticsValuesList.add(
//                    Pair(
//                        R.string.statisticsScreen_jitter_bufffer_target_delay,
//                        String.format("%.2f ms", it)
//                    )
//                )
//            }

            statistics.video?.jitterBufferMinimumDelay?.let { jbmd ->
                statistics.video?.jitterBufferEmittedCount?.let { jbec ->
                    statisticsValuesList.add(
                        Pair(
                            R.string.statisticsScreen_jitter_bufffer_min_delay,
                            String.format("%.2f ms", msNormalised(jbmd, jbec.toDouble()))
                        )
                    )
                }
            }

            statistics.video?.packetsLost?.let {
                statisticsValuesList.add(Pair(R.string.statisticsScreen_videoLoss, "$it"))
            }

            statistics.roundTripTime?.let {
                statisticsValuesList.add(
                    Pair(
                        R.string.statisticsScreen_rtt,
                        "${it.times(1000).toLong()} ms"
                    )
                )
            }

            statistics.timestamp?.let {
                statisticsValuesList.add(
                    Pair(
                        R.string.statisticsScreen_timestamp,
                        String.format("%.0f", it)
                    )
                )
            }

            statistics.video?.codecName?.let {
                statisticsValuesList.add(Pair(R.string.statisticsScreen_codecs, it))
            }

            return statisticsValuesList
        }
        return null
    }

    private fun getDateTime(timeStamp: Double): String? {
        return try {
            val dateFormat = SimpleDateFormat.getDateTimeInstance()
            val netDate = Date((timeStamp / 1000).toLong())
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

    private fun msNormalised(numerator: Double, denominator: Double): Double {
        return if (denominator == 0.0) {
            0.0
        } else {
            numerator * 1000 / denominator
        }
    }

    fun updateShowSimulcastSettings(show: Boolean) {
        _showSimulcastSettings.update { show }
    }

    fun selectStreamQualityType(streamQualityType: RTSViewerDataStore.StreamQualityType) {
        repository.selectStreamQualityType(streamQualityType)
    }
}
