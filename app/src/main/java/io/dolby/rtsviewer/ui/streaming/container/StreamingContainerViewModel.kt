package io.dolby.rtsviewer.ui.streaming.container

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.dolby.rtsviewer.preferenceStore.PrefsStore
import io.dolby.rtsviewer.ui.streaming.common.StreamError
import io.dolby.rtsviewer.ui.streaming.common.StreamInfo
import io.dolby.rtsviewer.ui.streaming.common.StreamStateInfo
import io.dolby.rtsviewer.ui.streaming.common.StreamingBridge
import io.dolby.rtsviewer.utils.NetworkStatusObserver
import io.dolby.rtsviewer.utils.StreamingConfig
import io.dolby.rtsviewer.utils.StreamingConfigData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StreamingContainerViewModel @Inject constructor(
    private val prefsStore: PrefsStore,
    private val networkStatusObserver: NetworkStatusObserver,
    private val streamingBridge: StreamingBridge
) : ViewModel() {
    private val _state = MutableStateFlow(StreamingContainerState())
    private val state: StateFlow<StreamingContainerState> = _state.asStateFlow()
    private val _uiState = MutableStateFlow(getRenderState())
    val uiState: StateFlow<StreamingContainerUiState> = _uiState.asStateFlow()

    private val config: StreamingConfig = HARDCODED_CONFIG // todo: or get from route

    init {
        viewModelScope.launch {
            launch {
                networkStatusObserver.status.distinctUntilChanged().collect {
                    when (it) {
                        NetworkStatusObserver.Status.Unavailable -> {
                            Log.d(TAG, "Unavailable network")
                            _state.update { state ->
                                state.copy(
                                    streamInfos = emptyList(),
                                    streamError = StreamError.NoInternetConnection,
                                    showLiveIndicator = prefsStore.isLiveIndicatorEnabled.first()
                                )
                            }
                        }
                        NetworkStatusObserver.Status.Available -> {
                            Log.d(TAG, "Available network")
                            val streamStateInfos = config.streams.mapIndexed { index, stream ->
                                StreamStateInfo(
                                    streamInfo = StreamInfo(
                                        index = index,
                                        apiUrl = stream.apiUrl,
                                        streamName = stream.streamName,
                                        accountId = stream.accountId
                                    )
                                )
                            }
                            _state.update { state ->
                                state.copy(
                                    streamInfos = streamStateInfos,
                                    streamError = null,
                                    showLiveIndicator = prefsStore.isLiveIndicatorEnabled.first()
                                )
                            }
                            streamingBridge.populateStreamStateInfos(streamStateInfos)
                        }
                    }
                    updateRenderState()
                }
            }
            launch {
                streamingBridge.streamStateInfos.collect { streamInfos ->
                    _state.update { it.copy(streamInfos = streamInfos) }
                    updateRenderState()
                }
            }
        }
    }

    fun onUiAction(action: StreamingContainerAction) {
        when (action) {
            is StreamingContainerAction.UpdateToolbarVisibility -> {
                _state.update { it.copy(showToolbarState = action.show) }
            }

            is StreamingContainerAction.UpdateSettingsVisibility -> {
                _state.update { it.copy(showSettings = action.show) }
            }

            is StreamingContainerAction.UpdateSimulcastSettingsVisibility -> {
                _state.update { it.copy(showSimulcastSettings = action.show) }
            }

            is StreamingContainerAction.UpdateStatisticsVisibility -> {
                _state.update { it.copy(showStatistics = action.show) }
                streamingBridge.updateShowStatistics(action.show)
            }

            is StreamingContainerAction.UpdateShowLiveVisibility -> {
                _state.update { it.copy(showLiveIndicator = action.show) }
                viewModelScope.launch {
                    prefsStore.updateLiveIndicator(action.show)
                }
            }

            is StreamingContainerAction.UpdateSelectedStreamQuality -> {
                _state.update { it.copy(selectedStreamQuality = action.streamQualityType) }
                streamingBridge.updateSelectedQuality(action.streamQualityType)
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
        return StreamingContainerUiState(
            streams = state.value.streamInfos.map { it.streamInfo },
            streamError = state.value.streamError,
            shouldStayOn = isSubscribed,
            isLive = isSubscribed,
            requestSettingsFocus = !isSubscribed,
            showSettings = state.value.showSettings,
            showSimulcastSettings = state.value.showSimulcastSettings,
            showLiveIndicator = state.value.showLiveIndicator,
            showToolbarState = state.value.showToolbarState,
            showStatistics = state.value.showStatistics && STATISTICS_BTN_SHOWN,
            statisticsEnabled = isSubscribed && STATISTICS_BTN_SHOWN,
            showSelectQualityBtn = QUALITY_SELECT_BTN_SHOWN,
            showStatisticsBtn = STATISTICS_BTN_SHOWN,
            selectedStreamQualityTitleId = state.value.selectedStreamQuality.titleResId,
            availableStreamQualityItems = state.value.streamInfos.flatMap { it.availableStreamQualities }.distinct(),
            simulcastSettingsEnabled = state.value.streamInfos.all { it.availableStreamQualities.isNotEmpty() }
        )
    }

    companion object {
        private const val TAG = "StreamContainerViewModel"
        private const val QUALITY_SELECT_BTN_SHOWN = false
        private const val STATISTICS_BTN_SHOWN = false
        private val HARDCODED_CONFIG = StreamingConfig(
            listOf(
                StreamingConfigData(
                    apiUrl = "https://director.millicast.com/api/director/subscribe",
                    streamName = "multiview",
                    accountId = "k9Mwad"
                ),
                StreamingConfigData(
                    apiUrl = "https://director.millicast.com/api/director/subscribe",
                    streamName = "game",
                    accountId = "7csQUs"
                ),
                StreamingConfigData(
                    apiUrl = "https://director.millicast.com/api/director/subscribe",
                    streamName = "game",
                    accountId = "7csQUs"
                ),
                StreamingConfigData(
                    apiUrl = "https://director.millicast.com/api/director/subscribe",
                    streamName = "multiview",
                    accountId = "k9Mwad"
                )
            )
        )
    }
}
