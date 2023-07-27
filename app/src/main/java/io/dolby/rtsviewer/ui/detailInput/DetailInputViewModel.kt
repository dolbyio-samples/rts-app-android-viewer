package io.dolby.rtsviewer.ui.detailInput

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import io.dolby.rtscomponentkit.data.RTSViewerDataStore
import io.dolby.rtscomponentkit.domain.StreamingData
import io.dolby.rtscomponentkit.utils.DispatcherProvider
import io.dolby.rtsviewer.datastore.RecentStreamsDataStore
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

    private val _useDevEnv = MutableStateFlow(false)
    val useDevEnv = _useDevEnv.asStateFlow()

    private val _disableAudio = MutableStateFlow(false)
    val disableAudio = _disableAudio.asStateFlow()

    private val _rtcLogs = MutableStateFlow(false)
    val rtcLogs = _rtcLogs.asStateFlow()

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

    fun connect() {
        defaultCoroutineScope.launch {
            repository.connect( StreamingData(
                streamName = streamName.value,
                accountId = accountId.value,
                useDevEnv = useDevEnv.value,
                disableAudio = disableAudio.value,
                rtcLogs = rtcLogs.value))

            if(!isDemo) {
                // Save the stream detail
                recentStreamsDataStore.addStreamDetail(streamName.value, accountId.value)
            }
        }
    }

    fun clearAllStreams() {
        defaultCoroutineScope.launch {
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

    fun updateUseDevEnv(state: Boolean) {
        _useDevEnv.value = state
    }

    fun updateDisableAudio(state: Boolean) {
        _disableAudio.value = state
    }

    fun updateRtcLogs(state: Boolean) {
        _rtcLogs.value = state
    }

    fun useDemoStream() {
        _streamName.value = "simulcastmultiview"
        _accountId.value = "k9Mwad"
        _useDevEnv.value = false
        _disableAudio.value = false
        _rtcLogs.value = false
    }
}
