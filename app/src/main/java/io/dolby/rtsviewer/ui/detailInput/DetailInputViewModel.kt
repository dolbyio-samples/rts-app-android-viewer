package io.dolby.rtsviewer.ui.detailInput

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import io.dolby.rtscomponentkit.data.RTSViewerDataStore
import io.dolby.rtscomponentkit.domain.StreamingData
import io.dolby.rtscomponentkit.utils.DispatcherProvider
import io.dolby.rtsviewer.datastore.RecentStreamsDataStore
import io.dolby.rtsviewer.datastore.StreamDetail
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DetailInputViewModel @Inject constructor(
    private val repository: RTSViewerDataStore,
    private val dispatcherProvider: DispatcherProvider,
    private val recentStreamsDataStore: RecentStreamsDataStore
) : ViewModel() {
    private val defaultCoroutineScope = CoroutineScope(dispatcherProvider.default)

    private val _uiState = MutableStateFlow(DetailInputScreenUiState())
    val uiState: StateFlow<DetailInputScreenUiState> = _uiState.asStateFlow()

    private val _streamName = MutableStateFlow("")
    var streamName = _streamName.asStateFlow()

    private val _accountId = MutableStateFlow("")
    var accountId = _accountId.asStateFlow()

    private var isDemo = false

    private val _useDevEnv = MutableStateFlow(true)
    val useDevEnv = _useDevEnv.asStateFlow()

    private val _disableAudio = MutableStateFlow(true)
    val disableAudio = _disableAudio.asStateFlow()

    private val _rtcLogs = MutableStateFlow(false)
    val rtcLogs = _rtcLogs.asStateFlow()

    private val _videoJitterMinimumDelayMs = MutableStateFlow(0f)
    val videoJitterMinimumDelayMs = _videoJitterMinimumDelayMs.asStateFlow()

    init {
        defaultCoroutineScope.launch {
            recentStreamsDataStore.recentStreams
                .collectLatest {
                    _uiState.update { state ->
                        state.copy(
                            recentStreams = it
                        )
                    }
                }
        }
    }

    fun connect() : StreamingData {
        val jb = videoJitterMinimumDelayMs.value.toInt()
        val sd = StreamingData(
            streamName = streamName.value,
            accountId = accountId.value,
            useDevEnv = useDevEnv.value,
            disableAudio = disableAudio.value,
            rtcLogs = rtcLogs.value,
            videoJitterMinimumDelayMs = jb
        )

        defaultCoroutineScope.launch {
            if (!isDemo) {
                // Save the stream detail
                recentStreamsDataStore.addStreamDetail(sd)
            }
        }
        return sd
    }

    fun resetStreamIfDemo() {
        if (isDemo) {
            isDemo = false
            _streamName.value = ""
            _accountId.value = ""
            _useDevEnv.value = true
            _disableAudio.value = true
            _rtcLogs.value = false
            _videoJitterMinimumDelayMs.value = 0f
        }
    }

    fun clearAllStreams() {
        defaultCoroutineScope.launch {
            recentStreamsDataStore.clearAll()
        }
    }

    val shouldPlayStream: Boolean
        get() = streamName.value.isNotEmpty() && accountId.value.isNotEmpty()

    fun useStreamingData(streamName: String) {
        defaultCoroutineScope.launch {
            recentStreamsDataStore.recentStream(streamName)?.let {
                updateStreamName(it.streamName)
                updateAccountId(it.accountID)
                updateUseDevEnv(it.useDevEnv)
                updateDisableAudio(it.disableAudio)
                updateRtcLogs(it.rtcLogs)
                updateJitterBufferMinimumDelay(it.videoJitterMinimumDelayMs.toFloat())
            }
        }
    }

    fun updateStreamName(name: String) {
        _streamName.value = name.trim()
    }

    fun updateAccountId(id: String) {
        _accountId.value = id.trim()
    }

    fun updateUseDevEnv(state: Boolean) {
        _useDevEnv.value = state
    }

    fun updateDisableAudio(state: Boolean) {
        _disableAudio.value = state
    }

    fun updateRtcLogs(state: Boolean) {
        _rtcLogs.value = state
    }

    fun updateJitterBufferMinimumDelay(delayMs: Float) {
        _videoJitterMinimumDelayMs.value = delayMs
    }

    fun useDemoStream() {
        isDemo = true
        _streamName.value = "simulcastmultiview"
        _accountId.value = "k9Mwad"
        _useDevEnv.value = false
        _disableAudio.value = false
        _rtcLogs.value = false
        _videoJitterMinimumDelayMs.value = 0f
    }
}
