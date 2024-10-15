package io.dolby.rtsviewer.ui.streaming.container

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.dolby.rtscomponentkit.domain.StreamConfig
import io.dolby.rtscomponentkit.domain.StreamConfigList
import io.dolby.rtsviewer.R
import io.dolby.rtsviewer.amino.AminoDevice
import io.dolby.rtsviewer.ui.streaming.common.StreamError
import io.dolby.rtsviewer.ui.streaming.common.StreamInfo
import io.dolby.rtsviewer.ui.streaming.common.StreamStateInfo
import io.dolby.rtsviewer.ui.streaming.common.StreamingBridge
import io.dolby.rtsviewer.utils.NetworkStatusObserver
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StreamingContainerViewModel @Inject constructor(
    private val networkStatusObserver: NetworkStatusObserver,
    private val streamingBridge: StreamingBridge,
    private val aminoDevice: AminoDevice
) : ViewModel() {
    private val _state = MutableStateFlow(StreamingContainerState())
    private val state: StateFlow<StreamingContainerState> = _state.asStateFlow()
    private val _uiState = MutableStateFlow(getRenderState())
    val uiState: StateFlow<StreamingContainerUiState> = _uiState.asStateFlow()

    private val config: StreamConfigList =
        aminoDevice.config.value ?: HARDCODED_CONFIG // todo: or get from route

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
                                    streamError = StreamError.NoInternetConnection
                                )
                            }
                        }

                        NetworkStatusObserver.Status.Available -> {
                            Log.d(TAG, "Available network")
                            val streamStateInfos =
                                config.streams.mapIndexed { index, stream ->
                                    StreamStateInfo(
                                        streamInfo = StreamInfo(
                                            index = index,
                                            apiUrl = "stream.apiUrl!!", // TODO remove
                                            streamName = stream.streamName,
                                            accountId = stream.accountId
                                        )
                                    )
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
            requestSettingsFocus = !isSubscribed,
            showSettings = state.value.streamInfos.any { it.shouldShowSettings },
            showSimulcastSettings = state.value.showSimulcastSettings,
            showStatistics = state.value.showStatistics && STATISTICS_BTN_SHOWN,
            statisticsEnabled = isSubscribed && STATISTICS_BTN_SHOWN,
            showStatisticsBtn = STATISTICS_BTN_SHOWN,
            selectedStreamQualityTitleId = state.value.streamInfos.find { it.shouldShowSettings }?.selectedStreamQuality?.titleResId
                ?: R.string.simulcast_auto,
            availableStreamQualityItems = state.value.streamInfos.find { it.shouldShowSettings }?.availableStreamQualities
                ?: emptyList(),
            simulcastSettingsEnabled = state.value.streamInfos.find { it.shouldShowSettings }?.availableStreamQualities?.isNotEmpty()
                ?: false
        )
    }

    companion object {
        private const val TAG = "StreamContainerViewModel"
        private const val STATISTICS_BTN_SHOWN = false
        private val HARDCODED_CONFIG: StreamConfigList =
            StreamConfigList(
                streams = listOf(
                    StreamConfig(
                        name = "Dolby",
                        desc = "Demo stream",
                        accountId = "k9Mwad",
                        streamName = "multiview"
                    )
                )
            )
    }
}
