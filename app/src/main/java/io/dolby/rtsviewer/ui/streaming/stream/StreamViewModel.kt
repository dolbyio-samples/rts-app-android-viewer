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
import com.millicast.subscribers.stats.SubscriberStats
import com.millicast.utils.MillicastException
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import io.dolby.rtscomponentkit.domain.StreamConfig
import io.dolby.rtsviewer.ui.streaming.common.AvailableStreamQuality
import io.dolby.rtsviewer.ui.streaming.common.StreamError
import io.dolby.rtsviewer.ui.streaming.common.StreamingBridge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.webrtc.VideoSink
import kotlin.time.Duration.Companion.milliseconds

@HiltViewModel(assistedFactory = StreamViewModel.Factory::class)
class StreamViewModel @AssistedInject constructor(
    @Assisted private val streamInfo: StreamConfig,
    private val streamingBridge: StreamingBridge
) : ViewModel() {

    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "Clearing viewmodel ${streamInfo.index}")
    }

    @AssistedFactory
    interface Factory {
        fun create(streamInfo: StreamConfig): StreamViewModel
    }

    private val _state = MutableStateFlow(StreamState())
    private val state: StateFlow<StreamState> = _state.asStateFlow()

    private val _uiState = MutableStateFlow(getRenderState())
    val uiState: StateFlow<StreamUiState> = _uiState.asStateFlow()

    private val _subscriberStats = MutableStateFlow<SubscriberStats?>(null)
    val subscriberStats: StateFlow<SubscriberStats?> = _subscriberStats.asStateFlow()

    private var subscriber: Subscriber? = null
    private var videoSink: VideoSink? = null

    init {
        Log.d(TAG, "INIT - ${streamInfo.index}")
        collectStreamingBridge()
    }

    private fun collectSubscriberStates() {
        viewModelScope.launch {
            subscriber?.state?.map { it.connectionState }?.distinctUntilChanged()
                ?.collect { connectionState ->
                    Log.d(TAG, "Subscriber state:: ${streamInfo.index} $connectionState; ${Thread.currentThread()}")
                    when (connectionState) {
                        is SubscriberConnectionState.Connected -> {
                            // No-op
                        }

                        is SubscriberConnectionState.Error -> {
                            Log.d(TAG, "Subscriber state error : ${streamInfo.index} ${connectionState.reason}; ${Thread.currentThread()}")
                            _state.update { it.copy(streamError = StreamError.StreamNotActive) }
                            updateRenderState()
                        }

                        SubscriberConnectionState.Connecting -> {
                            // No-op
                        }

                        SubscriberConnectionState.Disconnected -> {
                            // No-op
                        }

                        SubscriberConnectionState.DisconnectedError -> {
                            // No-op
                        }

                        SubscriberConnectionState.Disconnecting -> {
                            // No-op
                        }

                        SubscriberConnectionState.Stopped -> {
                            // No-op
                        }

                        SubscriberConnectionState.Subscribed -> {
                            _state.update { it.copy(streamError = null) }
                            updateRenderState()
                        }
                    }
                }
        }
        viewModelScope.launch {
            subscriber?.onRemoteTrack?.collect { track ->
                when (track) {
                    is RemoteAudioTrack -> {
                        if (state.value.audioTrack == null) {
                            withContext(Dispatchers.Default) {
                                Log.d(TAG, "Received Audio Track for ${streamInfo.index}; ${Thread.currentThread()}")
                                if (state.value.isFocused) {
                                    Log.d(TAG, "Scheduling audio track enable on default context ${streamInfo.index}; ${Thread.currentThread()}")
                                    track.setVolume(1.0)
                                    track.enableAsync()
                                }
                            }
                            viewModelScope.launch {
                                track.onState.map { it.isActive }.distinctUntilChanged()
                                    .collect { isActive ->
                                        if (isActive) {
                                            withContext(Dispatchers.Default) {
                                                Log.d(TAG, "Audio Track for channel ${streamInfo.index} is now active; ${Thread.currentThread()}")
                                                if (state.value.isFocused) {
                                                    videoSink?.let { sink ->
                                                        Log.d(TAG, "EnableAsync Audio Track; ${Thread.currentThread()}")
                                                        track.setVolume(1.0)
                                                        track.enableAsync()
                                                    }
                                                }
                                            }
                                        } else {
                                            withContext(Dispatchers.Default) {
                                                Log.d(TAG, "Audio Track for channel ${streamInfo.index} is now inactive; ${Thread.currentThread()}")
                                                Log.d(TAG, "DisableAsync Audio Track; ${Thread.currentThread()}")
                                                track.disableAsync()
                                            }
                                        }
                                        updateRenderState()
                                    }
                            }
                            _state.update { it.copy(audioTrack = track) }
                        }
                    }

                    is RemoteVideoTrack -> {
                        if (state.value.videoTrack == null) {
                            viewModelScope.launch {
                                track.onState.map { it.layers }.distinctUntilChanged().collect { layers ->
                                    val availableStreamQualities =
                                        layers?.activeLayers?.let {
                                            sortActiveLayers(it)
                                        } ?: run {
                                            emptyList()
                                        }
                                    Log.d(TAG, "on layer updates ${streamInfo.index}; ${Thread.currentThread()}")

                                    streamingBridge.updateAvailableSteamingQualities(
                                        streamInfo.index,
                                        availableStreamQualities
                                    )
                                }
                            }

                            viewModelScope.launch {
                                track.onState.map { it.isActive }.distinctUntilChanged()
                                    .collect { isActive ->
                                        if (isActive) {
                                            withContext(Dispatchers.Default) {
                                                Log.d(TAG, "Video Track for channel ${streamInfo.index} is now active; ${Thread.currentThread()}")
                                                videoSink?.let { sink ->
                                                    Log.d(TAG, "EnableAsync Video Track without layer; ${Thread.currentThread()}")
                                                    track.enableAsync(
                                                        promote = true,
                                                        layer = null,
                                                        videoSink = sink
                                                    )
                                                }
                                            }
                                            _state.update { it.copy(streamError = null) }
                                        } else {
                                            withContext(Dispatchers.Default) {
                                                Log.d(TAG, "Video Track for channel ${streamInfo.index} is now inactive; ${Thread.currentThread()}")
                                                Log.d(TAG, "DisableAsync Video Track; ${Thread.currentThread()}")
                                                track.disableAsync()
                                            }
                                            _state.update { it.copy(streamError = StreamError.StreamNotActive) }
                                        }
                                        updateRenderState()
                                    }
                            }
                            Log.d(TAG, "Received Video Track for ${streamInfo.index} status:${track.isActive}; ${Thread.currentThread()}")
                            _state.update { it.copy(videoTrack = track) }
                            updateRenderState()
                        }
                    }
                }
            }
        }

        viewModelScope.launch(Dispatchers.Default) {
            subscriber?.stats?.collect { stats ->
                _subscriberStats.value = stats
            }
        }
    }

    private fun connect() {
        viewModelScope.launch {
            withContext(Dispatchers.Default) {
                Log.d(TAG, "Connect Stream ${streamInfo.index}; ${Thread.currentThread()}")

                if (subscriber == null) {
                    Core.initialize()
                    subscriber = Core.createSubscriber()
                    collectSubscriberStates()
                }
                try {
                    val credentials =
                        Credential(streamInfo.streamName, streamInfo.accountId, streamInfo.directorUrl)
                    val connectionOptions = ConnectionOptions(true)
                    subscriber?.enableStats(true)
                    subscriber?.setCredentials(credentials)
                    subscriber?.connect(connectionOptions)
                    withContext(Dispatchers.Main) {
                        _state.update { it.copy(connected = true) }
                    }
                    subscriber?.subscribe(
                        options = Option(
                            forcePlayoutDelay = getForcePlayoutDelay(),
                            jitterMinimumDelayMs = streamInfo.jitterBufferDelay ?: 0,
                            forceSmooth = streamInfo.forceSmooth ?: false,
                        )
                    )
                    withContext(Dispatchers.Main) {
                        _state.update { it.copy(subscribed = true) }
                        streamingBridge.updateSubscribedState(streamInfo.index, true)
                        updateRenderState()
                    }
                } catch(e: MillicastException) {
                    withContext(Dispatchers.Main) {
                        _state.update {
                            it.copy(streamError = StreamError.StreamNotActive)
                        }
                        release()
                        // Schedule a reconnect in 5 seconds to check if the stream is online ??
                        scheduleReconnection()
                    }
                }
            }
        }
    }

    private fun scheduleReconnection() {
        viewModelScope.launch {
            withContext(Dispatchers.Default) {
                Log.d(TAG, "Schedule reconnection for stream ${streamInfo.index}; ${Thread.currentThread()}")
                repeat(1) {
                    delay(5000.milliseconds)
                    connect()
                }
            }
        }
    }

    private fun release() {
        _state.update {
            it.copy(
                connected = false,
                subscribed = false,
                disconnected = true,
                videoTrack = null,
                audioTrack = null
            )
        }
        updateRenderState()
        viewModelScope.launch {
            Log.d(TAG, "Release Stream ${streamInfo.index}; ${Thread.currentThread()}")
            withContext(Dispatchers.Default) {
                subscriber?.unsubscribe()
                subscriber?.disconnect()
                subscriber?.release()
                subscriber = null
            }
        }
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
            streamingBridge.streamStateInfos.collect { infos ->
                infos.find { it.streamInfo.index == streamInfo.index }?.selectedStreamQuality?.let { selectedStreamQuality ->
                    if (state.value.selectedStreamQuality != selectedStreamQuality) {
                        _state.update { it.copy(selectedStreamQuality = selectedStreamQuality) }
                        withContext(Dispatchers.Default) {
                            videoSink?.let { sink ->
                                Log.d(TAG, "Video Track enableAsync - Update layer!! ; ${Thread.currentThread()}")
                                state.value.videoTrack?.enableAsync(
                                    promote = true,
                                    layer = selectedStreamQuality.layerData,
                                    videoSink = sink
                                )
                            }
                        }
                    }
                }
                _state.update {
                    it.copy(isSingleStreamView = infos.count() == 1)
                }
                updateRenderState()
            }
        }
        viewModelScope.launch {
            streamingBridge.streamStateInfos.collect { infos ->
                Log.d(TAG, "Collect streaming statistics!! ; ${Thread.currentThread()}")
                val showStats =
                    infos.find { it.streamInfo.index == streamInfo.index }?.showStatistics
                _state.update {
                    it.copy(showStatistics = showStats ?: false)
                }
                updateRenderState()
            }
        }
        viewModelScope.launch {
            streamingBridge.streamStateInfos
                .map { it[streamInfo.index] }
                .distinctUntilChanged { old, new ->
                    old.isFocused == new.isFocused
                }
                .collect { streamStateInfo ->
                    _state.update { it.copy(isFocused = streamStateInfo.isFocused) }
                    updateRenderState()
                    withContext(Dispatchers.Default) {
                        Log.d(TAG, "Collect focus updates ${streamInfo.index}; ${Thread.currentThread()}")
                        state.value.audioTrack?.let {
                            if (streamStateInfo.isFocused) {
                                it.enableAsync()
                            } else {
                                it.disableAsync()
                            }
                        }
                    }
                }
        }
        viewModelScope.launch {
            streamingBridge.showLiveIndicator.collect { showLiveIndicator ->
                Log.d(TAG, "Collect Show Live Indicator state!! ; ${showLiveIndicator} ${Thread.currentThread()}")
                _state.update {
                    it.copy(shouldShowLiveIndicator = showLiveIndicator)
                }
                updateRenderState()
            }
        }
    }

    fun onUiAction(action: StreamAction) {
        when (action) {
            is StreamAction.Connect -> {
                Log.d(TAG, "Connect ${streamInfo.index}; ${Thread.currentThread()}")
                connect()
            }
            is StreamAction.Play -> {
                videoSink = action.videoSink
                viewModelScope.launch {
                    withContext(Dispatchers.Default) {
                        Log.d(TAG, "Play ${streamInfo.index}; ${Thread.currentThread()}")
                        state.value.videoTrack?.enableAsync(
                            promote = true,
                            layer = state.value.selectedStreamQuality.layerData,
                            videoSink = action.videoSink
                        )
                        Log.d(TAG, "Finished enabling track ${streamInfo.index}; ${Thread.currentThread()}")
                    }
                }
            }

            StreamAction.Pause -> {
                viewModelScope.launch {
                    withContext(Dispatchers.Default) {
                        Log.d(TAG, "Pause ${streamInfo.index}; ${Thread.currentThread()}")
                        state.value.videoTrack?.disableAsync()
                        state.value.audioTrack?.disableAsync()
                    }
                }
            }

            StreamAction.Release -> {
                Log.d(TAG, "Release ${streamInfo.index}")
                release()
            }

            is StreamAction.UpdateFocus -> {
                if (action.isFocused) {
                    Log.d(TAG, "Update focus ${streamInfo.index}")
                    streamingBridge.updateFocusedIndex(streamInfo.index)
                }
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
            audioTrack = state.value.audioTrack,
            selectedStreamQuality = state.value.selectedStreamQuality,
            showStatistics = state.value.showStatistics && state.value.subscribed,
            streamError = state.value.streamError,
            shouldShowLiveIndicator = state.value.shouldShowLiveIndicator,
            isSingleStreamView = state.value.isSingleStreamView
        )
    }

    private fun getForcePlayoutDelay(): ForcePlayoutDelay? {
        return streamInfo.forcePlayoutDelayMin?.let { min ->
            streamInfo.forcePlayoutDelayMax?.let { max ->
                ForcePlayoutDelay(min, max)
            }
        }
    }

    companion object {
        const val TAG = "StreamViewModel"
    }
}
