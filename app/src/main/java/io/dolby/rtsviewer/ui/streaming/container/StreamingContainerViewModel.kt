package io.dolby.rtsviewer.ui.streaming.container

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.millicast.utils.LogLevel
import dagger.hilt.android.lifecycle.HiltViewModel
import io.dolby.rtscomponentkit.domain.StreamConfig
import io.dolby.rtscomponentkit.domain.StreamConfigList
import io.dolby.rtsviewer.R
import io.dolby.rtsviewer.amino.RemoteConfigFlow
import io.dolby.rtsviewer.preferenceStore.PrefsStore
import io.dolby.rtsviewer.ui.streaming.common.StreamError
import io.dolby.rtsviewer.ui.streaming.common.StreamStateInfo
import io.dolby.rtsviewer.ui.streaming.common.StreamingBridge
import io.dolby.rtsviewer.utils.NetworkStatusObserver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class StreamingContainerViewModel @Inject constructor(
    private val networkStatusObserver: NetworkStatusObserver,
    private val streamingBridge: StreamingBridge,
    private val remoteConfigFlow: RemoteConfigFlow,
    private val preferencesDataStore: PrefsStore
    ) : ViewModel() {
    private val _state = MutableStateFlow(StreamingContainerState())
    private val state: StateFlow<StreamingContainerState> = _state.asStateFlow()
    private val _uiState = MutableStateFlow(getRenderState())
    val uiState: StateFlow<StreamingContainerUiState> = _uiState.asStateFlow()

    private val config: StreamConfigList = remoteConfigFlow.config.value

    init {
        viewModelScope.launch {
            networkStatusObserver.status.distinctUntilChanged().collect {
                when (it) {
                    NetworkStatusObserver.Status.Unavailable -> {
                        Log.d(TAG, "Unavailable network")
                        _state.update { state ->
                            state.copy(
                                streamInfos = emptyList(),
                                streamError = StreamError.NoInternetConnection
                            )
                        }
                    }

                    NetworkStatusObserver.Status.Available -> {
                        Log.d(TAG, "Available network")
                        val streamStateInfos =
                            config.streams.map { stream ->
                                StreamStateInfo(streamInfo = stream)
                            }
                        _state.update { state ->
                            state.copy(
                                streamInfos = streamStateInfos,
                                streamError = null
                            )
                        }
                        streamingBridge.populateStreamStateInfos(streamStateInfos)
                    }
                }
                updateRenderState()
            }
        }
        viewModelScope.launch {
            streamingBridge.streamStateInfos.collect { streamInfos ->
                _state.update { it.copy(streamInfos = streamInfos) }
                updateRenderState()
            }
        }
        viewModelScope.launch {
            preferencesDataStore.isLiveIndicatorEnabled.collect { enabled ->
                streamingBridge.updateShowLiveIndicator(enabled && _uiState.value.showLiveIndicatorSettings)
                _state.update { it.copy(liveIndicatorEnabled = enabled) }
                updateRenderState()
            }
        }
    }

    fun onUiAction(action: StreamingContainerAction) {
        when (action) {
            is StreamingContainerAction.UpdateSimulcastSettingsVisibility -> {
                _state.update { it.copy(showSimulcastSettings = action.show) }
            }

            is StreamingContainerAction.HideSettings -> {
                streamingBridge.hideSettings()
            }

            is StreamingContainerAction.UpdateStatisticsVisibility -> {
                _state.update { it.copy(showStatistics = action.show) }
                streamingBridge.updateShowStatistics(action.show)
            }

            is StreamingContainerAction.UpdateSelectedStreamQuality -> {
                streamingBridge.updateSelectedQuality(action.streamQualityType)
            }

            is StreamingContainerAction.UpdateLiveIndicatorVisibility -> {
                viewModelScope.launch {
                    withContext(Dispatchers.IO) {
                        preferencesDataStore.updateLiveIndicator(action.show)
                    }
                }
            }
        }
        updateRenderState()
    }

    private fun updateRenderState() {
        val uiState = getRenderState()
        _uiState.update { uiState }
    }

    private fun getRenderState(): StreamingContainerUiState {
        val isSubscribed = state.value.streamInfos.any { it.isSubscribed }
        val showStatistics = state.value.streamInfos.any {
            it.showStatistics && it.shouldShowSettings
        }

        return StreamingContainerUiState(
            streams = StreamConfigList(state.value.streamInfos.map { it.streamInfo }),
            streamError = state.value.streamError,
            shouldStayOn = isSubscribed,
            requestSettingsFocus = !isSubscribed,
            showSettings = state.value.streamInfos.any { it.shouldShowSettings },
            showSimulcastSettings = state.value.showSimulcastSettings,
            statisticsShown = showStatistics,
            statisticsEnabled = isSubscribed,
            liveIndicatorEnabled = state.value.liveIndicatorEnabled,
            selectedStreamQualityTitleId = state.value.streamInfos.find { it.shouldShowSettings }?.selectedStreamQuality?.titleResId
                ?: R.string.simulcast_auto,
            availableStreamQualityItems = state.value.streamInfos.find { it.shouldShowSettings }?.availableStreamQualities
                ?: emptyList(),
            simulcastSettingsEnabled = state.value.streamInfos.find { it.shouldShowSettings }?.availableStreamQualities?.isNotEmpty()
                ?: false,
            showLiveIndicatorSettings = state.value.streamInfos.count() == 1
        )
    }

    companion object {
        private const val TAG = "StreamContainerViewModel"

        private val HARD_CODED_CONFIG: StreamConfigList = StreamConfigList(
            listOf(
                StreamConfig(
                    directorUrl = "https://director.millicast.com/api/director/subscribe",
                    streamName = "multiview",
                    accountId = "k9Mwad",
                    desc = "test",
                    index = 0,
                    name = "channel 0",
                    logLevelWebSocket = LogLevel.MC_DEBUG,
                    logLevelWebRTC = LogLevel.MC_OFF
                ),
                StreamConfig(
                    directorUrl = "https://director.millicast.com/api/director/subscribe",
                    streamName = "multiview",
                    accountId = "k9Mwad",
                    desc = "test",
                    index = 1,
                    name = "channel 1",
                    logLevelWebSocket = LogLevel.MC_DEBUG,
                    logLevelWebRTC = LogLevel.MC_OFF
                ),
                StreamConfig(
                    directorUrl = "https://director.millicast.com/api/director/subscribe",
                    streamName = "multiview",
                    accountId = "k9Mwad",
                    desc = "test",
                    index = 2,
                    name = "channel 2",
                    logLevelWebSocket = LogLevel.MC_DEBUG,
                    logLevelWebRTC = LogLevel.MC_OFF
                ),
                StreamConfig(
                    directorUrl = "https://director.millicast.com/api/director/subscribe",
                    streamName = "multiview",
                    accountId = "k9Mwad",
                    desc = "test",
                    index = 3,
                    name = "channel 3",
                    logLevelWebSocket = LogLevel.MC_DEBUG,
                    logLevelWebRTC = LogLevel.MC_OFF
                )
            )
        )
    }
}
