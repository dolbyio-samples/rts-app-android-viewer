package io.dolby.interactiveplayer.detailInput

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.dolby.interactiveplayer.datastore.RecentStreamsDataStore
import io.dolby.interactiveplayer.preferenceStore.PrefsStore
import io.dolby.interactiveplayer.rts.data.MultiStreamingRepository
import io.dolby.interactiveplayer.rts.domain.ConnectOptions
import io.dolby.interactiveplayer.rts.domain.StreamingData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DetailInputViewModel @Inject constructor(
    private val recentStreamsDataStore: RecentStreamsDataStore,
    private val preferencesStore: PrefsStore
) : ViewModel() {
    private val _uiState = MutableStateFlow(DetailInputScreenUiState())
    val uiState: StateFlow<DetailInputScreenUiState> = _uiState.asStateFlow()

    private val _showDebugOptions = MutableStateFlow(false)
    val showDebugOptions = _showDebugOptions.asStateFlow()

    private val _selectedConnectionOptions = MutableStateFlow(ConnectOptions())
    val selectedConnectionOptions = _selectedConnectionOptions.asStateFlow()

    private val _showVideoQualityState = MutableStateFlow(false)
    val showVideoQualityState = _showVideoQualityState.asStateFlow()

    private val _streamName = MutableStateFlow("")
    var streamName = _streamName.asStateFlow()

    private val _accountId = MutableStateFlow("")
    var accountId = _accountId.asStateFlow()

    private var isDemo = false

    init {
        viewModelScope.launch {
            recentStreamsDataStore.recentStreams
                .collectLatest {
                    _uiState.update { state ->
                        state.copy(recentStreams = it)
                    }
                }
        }
        viewModelScope.launch {
            preferencesStore.showDebugOptions().collect { enabled ->
                _showDebugOptions.update { enabled }
            }
        }
    }

    fun updateSelectedConnectionOptions(options: ConnectOptions) {
        _selectedConnectionOptions.update {
            it.copy(
                useDevEnv = options.useDevEnv,
                forcePlayOutDelay = options.forcePlayOutDelay,
                disableAudio = options.disableAudio,
                rtcLogs = options.rtcLogs,
                primaryVideoQuality = options.primaryVideoQuality,
                videoJitterMinimumDelayMs = options.videoJitterMinimumDelayMs
            )
        }
    }

    fun saveSelectedStream() {
        val streamingData =
            StreamingData(streamName = streamName.value, accountId = accountId.value)
        viewModelScope.launch {
            if (!isDemo) {
                // Save the stream detail
                recentStreamsDataStore.addStreamDetail(
                    streamingData,
                    _selectedConnectionOptions.value
                )
            }
        }
    }

    fun clearAllStreams() {
        viewModelScope.launch {
            recentStreamsDataStore.clearAll()
        }
    }

    val shouldPlayStream: Boolean
        get() = streamName.value.isNotEmpty() && accountId.value.isNotEmpty()

    fun updateStreamName(name: String) {
        _streamName.value = name
    }

    fun updateAccountId(id: String) {
        _accountId.value = id
    }

    fun useDemoStream() {
        isDemo = true
        _streamName.value = DEMO_STREAM_NAME
        _accountId.value = DEMO_ACCOUNT_ID
    }

    fun updateJitterBufferMinimumDelay(value: Int) {
        _selectedConnectionOptions.update {
            it.copy(videoJitterMinimumDelayMs = value)
        }
    }

    fun updateUseDevEnv(value: Boolean) {
        _selectedConnectionOptions.update {
            it.copy(useDevEnv = value)
        }
    }

    fun updateForcePlayOutDelay(value: Boolean) {
        _selectedConnectionOptions.update {
            it.copy(forcePlayOutDelay = value)
        }
    }

    fun updateDisableAudio(value: Boolean) {
        _selectedConnectionOptions.update {
            it.copy(disableAudio = value)
        }
    }

    fun updateRtcLogs(value: Boolean) {
        _selectedConnectionOptions.update {
            it.copy(rtcLogs = value)
        }
    }

    fun showPrimaryVideoQualitySelection(show: Boolean) {
        _showVideoQualityState.update { show }
    }

    fun togglePrimaryVideoQualitySelection() {
        _showVideoQualityState.update { !it }
    }

    fun videoQualities(): Array<MultiStreamingRepository.VideoQuality> =
        MultiStreamingRepository.VideoQuality.values()

    fun updatePrimaryVideoQuality(videoQuality: MultiStreamingRepository.VideoQuality) {
        _selectedConnectionOptions.update {
            it.copy(primaryVideoQuality = videoQuality)
        }
    }
}

const val DEMO_STREAM_NAME = "simulcastmultiview"
const val DEMO_ACCOUNT_ID = "k9Mwad"
