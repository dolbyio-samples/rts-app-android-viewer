package io.dolby.rtsviewer.ui.detailInput

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.millicast.Core
import com.millicast.utils.LogLevel
import com.millicast.utils.Logger
import com.squareup.moshi.Moshi
import dagger.hilt.android.lifecycle.HiltViewModel
import io.dolby.rtscomponentkit.data.RemoteConfigService
import io.dolby.rtscomponentkit.domain.MediaServerEnv
import io.dolby.rtscomponentkit.domain.StreamConfig
import io.dolby.rtscomponentkit.domain.StreamConfigList
import io.dolby.rtscomponentkit.domain.StreamingData
import io.dolby.rtsviewer.amino.AminoDeviceRemoteService
import io.dolby.rtsviewer.amino.RemoteConfigFlow
import io.dolby.rtsviewer.datastore.RecentStreamsDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

private const val TAG = "DetailInputViewModel"
@HiltViewModel
class DetailInputViewModel @Inject constructor(
    private val aminoService: AminoDeviceRemoteService,
    private val recentStreamsDataStore: RecentStreamsDataStore,
    private val remoteConfigFlow: RemoteConfigFlow,
    private val moshi: Moshi
) : ViewModel() {

    companion object {
        const val DEMO_STREAM_NAME = StreamingData.DEMO_STREAM_NAME
        const val DEMO_ACCOUNT_ID = StreamingData.DEMO_ACCOUNT_ID
    }

    private val _uiState = MutableStateFlow(DetailInputScreenUiState())
    val uiState: StateFlow<DetailInputScreenUiState> = _uiState.asStateFlow()

    private val _streamName = MutableStateFlow("")
    var streamName = _streamName.asStateFlow()

    private val _accountId = MutableStateFlow("")
    var accountId = _accountId.asStateFlow()

    private val _remoteConfigUrl = MutableStateFlow("")
    var remoteConfigUrl = _remoteConfigUrl.asStateFlow()

    init {
        Core.initialize()

        // set millicast logs
        Logger.setLogLevels(
            sdk = LogLevel.MC_DEBUG,
            webrtc = LogLevel.MC_DEBUG,
            websocket = LogLevel.MC_OFF
        )
        Logger.setLoggerListener { msg, level ->
            Log.i(TAG, "Millicast sdk: $level / $msg")
        }
        viewModelScope.launch {
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

    fun useAminoService() {
        remoteConfigFlow.updateConfig(aminoService.configList)
    }

    suspend fun connect(selectedMediaServerEnv: MediaServerEnv, isDemo: Boolean) {
        withContext(Dispatchers.Default) {
            val streamConfigList = List(1) { index ->
                StreamConfig(
                    index = index,
                    directorUrl = selectedMediaServerEnv.getURL(),
                    name = "SingleView",
                    desc = "Single view mode",
                    streamName = if (isDemo) DEMO_STREAM_NAME else streamName.value,
                    accountId = if (isDemo) DEMO_ACCOUNT_ID else accountId.value
                )
            }
            remoteConfigFlow.updateConfig(StreamConfigList(streamConfigList))

            if (!isDemo) {
                // Save the stream detail
                recentStreamsDataStore.addStreamDetail(streamName.value, accountId.value)
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

    fun updateRemoteConfigUrl(name: String) {
        _remoteConfigUrl.value = name
    }

    suspend fun useDemoStream() {
        connect(selectedMediaServerEnv = MediaServerEnv.PROD, isDemo = true)
    }

    fun listOfEnv() = MediaServerEnv.listOfEnv()

    val isAminoDevice: Boolean = aminoService.configList.streams.isNotEmpty()

    fun getRemoteConfig() {
        if (!remoteConfigUrl.value.startsWith("https://")) {
            _uiState.update { state ->
                state.copy(remoteConfigFetchState = RemoteConfigFetchState.ERROR)
            }
            return
        }
        viewModelScope.launch {
            _uiState.update { state ->
                state.copy(remoteConfigFetchState = RemoteConfigFetchState.FETCHING) // TODO show spinner
            }

            withContext(Dispatchers.Default) {
                val service = RemoteConfigService(remoteConfigUrl.value, moshi)
                service.fetch()?.let { config ->
                    val streamConfigList = List(config.url.size) { index ->
                        StreamConfig.from(config, index = index)
                    }

                    withContext(Dispatchers.Main) {
                        Log.d(TAG, "Remote config fetch $streamConfigList")
                        remoteConfigFlow.updateConfig(StreamConfigList(streamConfigList))
                        _uiState.update { state ->
                            state.copy(remoteConfigFetchState = RemoteConfigFetchState.SUCCESS)
                        }
                    }
                } ?: run {
                    withContext(Dispatchers.Main) {
                        _uiState.update { state ->
                            state.copy(remoteConfigFetchState = RemoteConfigFetchState.ERROR)
                        }
                    }
                }
            }
        }
    }

    fun updateRemoteConfigFetchState(newState: RemoteConfigFetchState) {
        _uiState.update { state ->
            state.copy(remoteConfigFetchState = newState)
        }
    }
}
