package io.dolby.rtsviewer.ui.streaming.stream

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.millicast.Core
import com.millicast.Subscriber
import com.millicast.clients.ConnectionOptions
import com.millicast.subscribers.Credential
import com.millicast.subscribers.ForcePlayoutDelay
import com.millicast.subscribers.Option
import com.millicast.subscribers.remote.RemoteAudioTrack
import com.millicast.subscribers.remote.RemoteVideoTrack
import com.millicast.subscribers.state.LayerDataSelection
import com.millicast.subscribers.state.SubscriberConnectionState
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import io.dolby.rtscomponentkit.data.multistream.safeLaunch
import io.dolby.rtsviewer.ui.streaming.common.AvailableStreamQuality
import io.dolby.rtsviewer.ui.streaming.common.StreamError
import io.dolby.rtsviewer.ui.streaming.common.StreamInfo
import io.dolby.rtsviewer.ui.streaming.common.StreamingBridge
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.webrtc.VideoSink

@HiltViewModel(assistedFactory = StreamViewModel.Factory::class)
class StreamViewModel @AssistedInject constructor(
    @Assisted private val streamInfo: StreamInfo,
    private val streamingBridge: StreamingBridge
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(streamInfo: StreamInfo): StreamViewModel
    }

    private val _state = MutableStateFlow(StreamState())
    private val state: StateFlow<StreamState> = _state.asStateFlow()
    private val _uiState = MutableStateFlow(getRenderState())
    val uiState: StateFlow<StreamUiState> = _uiState.asStateFlow()

    private var subscriber: Subscriber? = null
    private var videoSink: VideoSink? = null

    init {
        Log.d(TAG, "INIT - ${streamInfo.index}")
        collectStreamingBridge()
    }

    private fun collectSubscriberStates() {
        viewModelScope.launch {
            launch {
                subscriber?.state?.map { it.connectionState }?.distinctUntilChanged()
                    ?.collect { connectionState ->
                        Log.d(TAG, "ConnectionState : $connectionState")
                        when (connectionState) {
                            is SubscriberConnectionState.Connected -> {
                                subscriber?.subscribe(
                                    options = Option(
                                        disableAudio = false,
                                        forcePlayoutDelay = ForcePlayoutDelay(
                                            minimumDelay = 0,
                                            maximumDelay = 1
                                        ),
                                        jitterMinimumDelayMs = 0
                                    )
                                )
                            }

                            is SubscriberConnectionState.Error -> {
                                Log.d(TAG, "Error : ${connectionState.reason}")
                                _state.update { it.copy(streamError = StreamError.StreamNotActive) }
                            }

                            else -> {}
                        }
                        val isSubscribed = connectionState == SubscriberConnectionState.Subscribed
                        if (state.value.subscribed != isSubscribed) {
                            _state.update { it.copy(subscribed = isSubscribed) }
                            streamingBridge.updateSubscribedState(streamInfo.index, isSubscribed)
                            updateRenderState()
                        }
                    }
            }
            launch {
                subscriber?.onRemoteTrack?.distinctUntilChanged()?.collectLatest { track ->
                    when (track) {
                        is RemoteAudioTrack -> {
                            if (state.value.audioTrack == null) {
                                Log.d(TAG, "Received Audio Track for ${streamInfo.index}")
                                if (state.value.isFocused) {
                                    track.setVolume(1.0)
                                    track.enableAsync()
                                } else {
                                    track.setVolume(0.0)
                                    track.disableAsync()
                                }
                                _state.update { it.copy(audioTrack = track) }
                            }
                        }

                        is RemoteVideoTrack -> {
                            if (state.value.videoTrack == null) {
                                Log.d(TAG, "Received Video Track for ${streamInfo.index}")
                                _state.update { it.copy(videoTrack = track) }
                                track.onState.collect { trackState ->
                                    val availableStreamQualities =
                                        trackState.layers?.activeLayers?.let {
                                            sortActiveLayers(it)
                                        } ?: run {
                                            emptyList()
                                        }
                                    streamingBridge.updateAvailableSteamingQualities(
                                        streamInfo.index,
                                        availableStreamQualities
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun connect() {
        Log.d(TAG, "Connect Stream ${streamInfo.index}")
        viewModelScope.safeLaunch(block = {
            subscriber = Core.createSubscriber()
            collectSubscriberStates()
            val credentials =
                Credential(streamInfo.streamName, streamInfo.accountId, streamInfo.apiUrl)
            val connectionOptions = ConnectionOptions(true)
            subscriber?.enableStats(true)
            subscriber?.setCredentials(credentials)
            subscriber?.connect(connectionOptions)
        }) {
            _state.update {
                it.copy(streamError = StreamError.StreamNotActive)
            }
            release()
        }
    }

    private fun release() {
        Log.d(TAG, "Release Stream ${streamInfo.index}")
        _state.update {
            it.copy(
                subscribed = false,
                disconnected = true,
                videoTrack = null,
                audioTrack = null
            )
        }
        updateRenderState()
        subscriber?.disconnect()
        subscriber?.release()
        subscriber = null
    }

    private fun sortActiveLayers(activeLayers: List<LayerDataSelection>): List<AvailableStreamQuality> {
        val filteredActiveLayers = mutableListOf<LayerDataSelection>()
        var simulcastLayers = activeLayers.filter { it.encodingId?.isNotEmpty() == true }
        if (simulcastLayers.isNotEmpty()) {
            val grouped = simulcastLayers.groupBy { it.encodingId }
            grouped.values.forEach { layers ->
                val layerWithBestFrameRate =
                    layers.firstOrNull { it.temporalLayerId == it.maxTemporalLayerId }
                        ?: layers.last()
                filteredActiveLayers.add(layerWithBestFrameRate)
            }
        } else {
            simulcastLayers = activeLayers.filter { it.spatialLayerId != null }
            val grouped = simulcastLayers.groupBy { it.spatialLayerId }
            grouped.values.forEach { layers ->
                val layerWithBestFrameRate =
                    layers.firstOrNull { it.spatialLayerId == it.maxSpatialLayerId }
                        ?: layers.last()
                filteredActiveLayers.add(layerWithBestFrameRate)
            }
        }

        filteredActiveLayers.sortWith(object : Comparator<LayerDataSelection> {
            override fun compare(o1: LayerDataSelection?, o2: LayerDataSelection?): Int {
                if (o1 == null) return -1
                if (o2 == null) return 1
                return when (o2.encodingId?.lowercase()) {
                    "h" -> -1
                    "l" -> if (o1.encodingId?.lowercase() == "h") -1 else 1
                    "m" -> if (o1.encodingId?.lowercase() == "h" || o1.encodingId?.lowercase() != "l") -1 else 1
                    else -> 1
                }
            }
        })

        val trackLayerDataList = when {
            filteredActiveLayers.count() == 2 -> listOf(
                AvailableStreamQuality.AUTO,
                AvailableStreamQuality.High(filteredActiveLayers[0]),
                AvailableStreamQuality.Low(filteredActiveLayers[1])
            )

            filteredActiveLayers.count() >= 3 -> listOf(
                AvailableStreamQuality.AUTO,
                AvailableStreamQuality.High(filteredActiveLayers[0]),
                AvailableStreamQuality.Medium(filteredActiveLayers[1]),
                AvailableStreamQuality.Low(filteredActiveLayers[2])
            )

            else -> listOf(
                AvailableStreamQuality.AUTO
            )
        }
        return trackLayerDataList
    }

    private fun collectStreamingBridge() {
        viewModelScope.launch {
            launch {
                viewModelScope.launch {
                    streamingBridge.streamStateInfos.collect { infos ->
                        infos.find { it.streamInfo.index == streamInfo.index }?.selectedStreamQuality?.let { selectedStreamQuality ->
                            if (state.value.selectedStreamQuality != selectedStreamQuality) {
                                videoSink?.let { sink ->
                                    state.value.videoTrack?.enableAsync(
                                        promote = true,
                                        layer = selectedStreamQuality.layerData,
                                        videoSink = sink
                                    )
                                }
                                _state.update { it.copy(selectedStreamQuality = selectedStreamQuality) }
                            }
                        }
                    }
                }
            }
            launch {
                viewModelScope.launch {
                    streamingBridge.showStatistics.collect { showStatistics ->
                        _state.update {
                            it.copy(
                                showStatistics = showStatistics, /*remove*/
                                subscribed = showStatistics
                            )
                        }
                        updateRenderState()
                    }
                }
            }
        }
    }

    fun onUiAction(action: StreamAction) {
        when (action) {
            StreamAction.Connect -> connect()
            is StreamAction.Play -> {
                videoSink = action.videoSink
                state.value.videoTrack?.enableAsync(
                    promote = true,
                    layer = state.value.selectedStreamQuality.layerData,
                    videoSink = action.videoSink
                )
                if (state.value.isFocused) {
                    state.value.audioTrack?.enableAsync()
                }
            }

            StreamAction.Pause -> {
                state.value.videoTrack?.disableAsync()
                state.value.audioTrack?.disableAsync()
            }

            StreamAction.Release -> release()
            is StreamAction.UpdateFocus -> {
                _state.update { it.copy(isFocused = action.isFocused) }
                viewModelScope.launch {
                    state.value.audioTrack?.let {
                        if (action.isFocused) {
                            it.setVolume(1.0)
                            it.enableAsync()
                        } else {
                            it.setVolume(0.0)
                            it.disableAsync()
                        }
                    }
                }
                updateRenderState()
            }
            is StreamAction.UpdateSettingsVisibility -> {
                streamingBridge.updateShowSettings(streamInfo.index, action.show)
            }
        }
    }

    private fun updateRenderState() {
        val uiState = getRenderState()
        _uiState.update { uiState }
    }

    private fun getRenderState(): StreamUiState {
        return StreamUiState(
            shouldRequestFocusInitially = streamInfo.index == 0,
            showSettingsButton = state.value.isFocused,
            isFocused = state.value.isFocused,
            subscribed = state.value.subscribed,
            videoTrack = state.value.videoTrack,
            selectedStreamQuality = state.value.selectedStreamQuality,
            showStatistics = state.value.showStatistics && state.value.subscribed,
            streamError = state.value.streamError
        )
    }

    companion object {
        const val TAG = "StreamViewModel"
    }
}
