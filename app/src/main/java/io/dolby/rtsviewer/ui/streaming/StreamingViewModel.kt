package io.dolby.rtsviewer.ui.streaming

import android.icu.text.SimpleDateFormat
import android.os.CountDownTimer
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.dolby.rtscomponentkit.data.RTSViewerDataStore
import io.dolby.rtscomponentkit.data.SingleStreamStatisticsData
import io.dolby.rtscomponentkit.data.multistream.safeLaunch
import io.dolby.rtscomponentkit.domain.StreamingData
import io.dolby.rtscomponentkit.utils.DispatcherProvider
import io.dolby.rtsviewer.R
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
    private var currentStreamIndex = 0
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

    private var channels = arrayListOf(
        StreamingData("MG2zym", "amino50fps"),
        StreamingData("MG2zym", "amino25fps"),
        StreamingData("7csQUs", "game")
    )
    private var alreadyCleared = false
    private var switchingChannelsTimer: CountDownTimer? = object : CountDownTimer(50, 1000) {
        override fun onTick(millisUntilFinished: Long) {
        }

        override fun onFinish() {
            subscribeToNewChannel()
        }
    }

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

    fun switchChannel(navDirection: ChannelNavDirection) {
        channels.let { allChannels ->
            if (allChannels.isNotEmpty()) {
                repository.disconnect()
                viewModelScope.launch {
                    _uiState.emit(StreamingScreenUiState())
                }
                _uiState.value.videoTrack?.setEnabled(false)
                _uiState.value.audioTrack?.setEnabled(false)
                Log.e(TAG, "SwitchChanneeeel removeAllVideoSinksFinished")
                currentStreamIndex = if (navDirection == ChannelNavDirection.DOWN) {
                    (currentStreamIndex + 1).coerceInLoop(allChannels.indices)
                } else {
                    (currentStreamIndex - 1).coerceInLoop(allChannels.indices)
                }
                startTimer()
            }
        }
    }

    override fun onCleared() {
        releaseTimer()
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

    private fun startTimer() {
        switchingChannelsTimer?.cancel()
        switchingChannelsTimer?.start()
    }

    private fun releaseTimer() {
        switchingChannelsTimer?.cancel()
        switchingChannelsTimer = null
    }

    fun subscribeToNewChannel() {
        viewModelScope.safeLaunch(block = {
            repository.connect(
                channels[currentStreamIndex].streamName,
                channels[currentStreamIndex].accountId
            )
            updateShowSimulcastSettings(false)
            settingsVisibility(false)
        })
    }

    private fun streamingStatistics(): Flow<List<Pair<Int, String>>?> =
        repository.statisticsData.map { statisticsData -> getStatisticsValuesList(statisticsData) }

    private fun getStatisticsValuesList(statisticsData: SingleStreamStatisticsData?): List<Pair<Int, String>>? {
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
            statistics.audio?.bytesReceived?.let {
                statisticsValuesList.add(
                    Pair(
                        R.string.statisticsScreen_audioTotal,
                        formattedByteCount(it.toLong())
                    )
                )
            }
            statistics.video?.packetsLost?.let {
                statisticsValuesList.add(Pair(R.string.statisticsScreen_videoLoss, "$it"))
            }
            statistics.audio?.packetsLost?.let {
                statisticsValuesList.add(Pair(R.string.statisticsScreen_audioLoss, "$it"))
            }
            statistics.video?.jitter?.let {
                statisticsValuesList.add(
                    Pair(
                        R.string.statisticsScreen_videoJitter,
                        "${it.times(1000)} ms"
                    )
                )
            }
            statistics.audio?.jitter?.let {
                statisticsValuesList.add(
                    Pair(
                        R.string.statisticsScreen_audioJitter,
                        "${it.times(1000)} ms"
                    )
                )
            }
            var codecNames = ""
            statistics.video?.codecName?.let {
                codecNames += it
            }
            statistics.audio?.codecName?.let {
                if (codecNames.isNotEmpty()) codecNames += ", "
                codecNames += it
            }
            if (codecNames.isNotEmpty()) {
                statisticsValuesList.add(Pair(R.string.statisticsScreen_codecs, codecNames))
            }
            statistics.timestamp?.let {
                getDateTime(it)?.let { dateTime ->
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
        repository.selectStreamQualityType(streamQualityType)
    }
}

fun Int.coerceInLoop(range: ClosedRange<Int>): Int {
    if (range is ClosedFloatingPointRange) {
        return this.coerceIn<Int>(range)
    }
    if (range.isEmpty()) throw IllegalArgumentException("Cannot coerce value to an empty range: $range.")
    return when {
        this < range.start -> range.endInclusive
        this > range.endInclusive -> range.start
        else -> this
    }
}
