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
import com.millicast.subscribers.state.SubscriberConnectionState
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import io.dolby.rtscomponentkit.data.multistream.safeLaunch
import io.dolby.rtsviewer.ui.streaming.common.StreamError
import io.dolby.rtsviewer.ui.streaming.common.StreamInfo
import io.dolby.rtsviewer.ui.streaming.common.StreamingBridge
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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
                subscriber?.onRemoteTrack?.distinctUntilChanged()?.collect { track ->
                    when (track) {
                        is RemoteAudioTrack -> {
                            if (state.value.audioTrack == null) {
                                Log.d(TAG, "Received Audio Track for ${streamInfo.index}")
                                if (streamInfo.index == 0) {
                                    track.setVolume(1.0)
                                    track.enableAsync()
                                } else {
                                    track.setVolume(0.0)
                                    track.disableAsync()
                                }
                            }
                        }

                        is RemoteVideoTrack -> {
                            if (state.value.videoTrack == null) {
                                Log.d(TAG, "Received Video Track for ${streamInfo.index}")
                                _state.update { it.copy(videoTrack = track) }
                                updateRenderState()
                            }
                        }
                    }
                }
            }
            launch {
//                subscriber?.layers?.distinctUntilChanged()?.collect { layers ->
//                    Log.d(TAG, "Received layers for ${streamInfo.index}")
//                    val definedStreamQualities = layers.activeLayers.mapNotNull {
//                        when (it.encodingId) {
//                            "h" -> AvailableStreamQuality.High(it)
//                            "m" -> AvailableStreamQuality.Medium(it)
//                            "l" -> AvailableStreamQuality.Low(it)
//                            else -> null
//                        }
//                    }
//                    val availableStreamQualities = mutableListOf<AvailableStreamQuality>(AvailableStreamQuality.AUTO)
//                    availableStreamQualities.addAll(definedStreamQualities)
//                    streamingBridge.updateAvailableSteamingQualities(streamInfo.index, availableStreamQualities)
//                }
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

    private fun collectStreamingBridge() {
        viewModelScope.launch {
            launch {
                streamingBridge.selectedStreamQuality.collect { quality ->
                    _state.update { it.copy(selectedStreamQuality = quality) }
                    updateRenderState()
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
                state.value.videoTrack?.enableAsync(
                    promote = true,
                    layer = null,
                    videoSink = action.videoSink
                )
                if (streamInfo.index == 0) {
                    state.value.audioTrack?.enableAsync()
                }
            }

            StreamAction.Pause -> {
                state.value.videoTrack?.disableAsync()
                state.value.audioTrack?.disableAsync()
            }

            StreamAction.Release -> release()
        }
    }

    private fun updateRenderState() {
        val uiState = getRenderState()
        _uiState.update { uiState }
    }

    private fun getRenderState(): StreamUiState {
        return StreamUiState(
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
