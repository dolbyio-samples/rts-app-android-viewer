package io.dolby.interactiveplayer.detailInput

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.dolby.interactiveplayer.datastore.RecentStreamsDataStore
import io.dolby.rtscomponentkit.data.RTSViewerDataStore
import io.dolby.rtscomponentkit.data.multistream.MultiStreamingRepository
import io.dolby.rtscomponentkit.data.multistream.VideoQuality
import io.dolby.rtscomponentkit.data.multistream.prefs.MultiStreamPrefsStore
import io.dolby.rtscomponentkit.domain.ConnectOptions
import io.dolby.rtscomponentkit.domain.ENV
import io.dolby.rtscomponentkit.domain.StreamingData
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
    private val preferencesStore: MultiStreamPrefsStore,
    private val repository: MultiStreamingRepository
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
                if (!enabled) {
                    _selectedConnectionOptions.update { ConnectOptions() }
                }
            }
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
        isDemo = false
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

    fun updateUseEnv(value: ENV) {
        _selectedConnectionOptions.update {
            it.copy(serverEnv = value)
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

    fun videoQualities(): Array<VideoQuality> = VideoQuality.values()

    fun updatePrimaryVideoQuality(videoQuality: VideoQuality) {
        _selectedConnectionOptions.update {
            it.copy(primaryVideoQuality = videoQuality)
        }
    }

    fun listOfEnv() = repository.listOfEnv()
}

const val DEMO_STREAM_NAME = StreamingData.DEMO_STREAM_NAME
const val DEMO_ACCOUNT_ID = StreamingData.DEMO_ACCOUNT_ID
