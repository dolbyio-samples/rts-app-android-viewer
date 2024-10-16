package io.dolby.rtsviewer.ui.detailInput

import androidx.lifecycle.ViewModel
import com.squareup.moshi.Moshi
import dagger.hilt.android.lifecycle.HiltViewModel
import io.dolby.rtscomponentkit.data.RTSViewerDataStore
import io.dolby.rtscomponentkit.data.RemoteConfigService
import io.dolby.rtscomponentkit.domain.MediaServerEnv
import io.dolby.rtscomponentkit.domain.StreamConfig
import io.dolby.rtscomponentkit.domain.StreamConfigList
import io.dolby.rtscomponentkit.domain.StreamingData
import io.dolby.rtscomponentkit.utils.DispatcherProvider
import io.dolby.rtsviewer.amino.RemoteConfigFlow
import io.dolby.rtsviewer.datastore.RecentStreamsDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class DetailInputViewModel @Inject constructor(
    private val repository: RTSViewerDataStore,
    private val dispatcherProvider: DispatcherProvider,
    private val recentStreamsDataStore: RecentStreamsDataStore,
    private val remoteConfigFlow: RemoteConfigFlow,
    private val moshi: Moshi
) : ViewModel() {

    companion object {
        const val DEMO_STREAM_NAME = StreamingData.DEMO_STREAM_NAME
        const val DEMO_ACCOUNT_ID = StreamingData.DEMO_ACCOUNT_ID
    }

    private val defaultCoroutineScope = CoroutineScope(dispatcherProvider.default)

    private val _uiState = MutableStateFlow(DetailInputScreenUiState())
    val uiState: StateFlow<DetailInputScreenUiState> = _uiState.asStateFlow()

    private val _streamName = MutableStateFlow("")
    var streamName = _streamName.asStateFlow()

    private val _accountId = MutableStateFlow("")
    var accountId = _accountId.asStateFlow()

    private val _remoteConfigUrl =
        MutableStateFlow("https://aravind-raveendran.github.io/remote-configs/config.json")
    var remoteConfigUrl = _remoteConfigUrl.asStateFlow()

    private var isDemo = false

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

    suspend fun connect(selectedMediaServerEnv: MediaServerEnv): Boolean =
        withContext(dispatcherProvider.default) {
            val connected = repository.connect(
                selectedMediaServerEnv,
                StreamingData(accountId = accountId.value, streamName = streamName.value)
            )

            if (connected && !isDemo) {
                // Save the stream detail
                recentStreamsDataStore.addStreamDetail(streamName.value, accountId.value)
            }

            isDemo = false
            return@withContext connected
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

    fun useDemoStream() {
        isDemo = true
        _streamName.value = DEMO_STREAM_NAME
        _accountId.value = DEMO_ACCOUNT_ID
    }

    fun listOfEnv() = MediaServerEnv.listOfEnv()

    val isAminoDevice: Boolean
        get() = remoteConfigFlow.config.value.streams.isNotEmpty()

    fun getRemoteConfig() {
        if(!remoteConfigUrl.value.startsWith("https://")){
            _uiState.update { state ->
                state.copy(remoteConfigFetchState = RemoteConfigFetchState.ERROR)
            }
            return
        }
        defaultCoroutineScope.launch {
            _uiState.update { state ->
                state.copy(remoteConfigFetchState = RemoteConfigFetchState.FETCHING) // TODO show spinner
            }

            val service = RemoteConfigService(remoteConfigUrl.value, moshi)
            service.fetch()?.let { config ->
                val streamConfigList = List(config.url.size) { index ->
                    StreamConfig.from(config, index = index)
                }

                remoteConfigFlow.updateConfig(StreamConfigList(streamConfigList))
                _uiState.update { state ->
                    state.copy(remoteConfigFetchState = RemoteConfigFetchState.SUCCESS)
                }
            } ?: run {
                _uiState.update { state ->
                    state.copy(remoteConfigFetchState = RemoteConfigFetchState.ERROR)
                }
            }
        }
    }

    fun updateRemoteConfigFetchState(newState : RemoteConfigFetchState) {
        _uiState.update { state ->
            state.copy(remoteConfigFetchState = newState)
        }
    }
}
