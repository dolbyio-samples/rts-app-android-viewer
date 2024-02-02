package io.dolby.rtscomponentkit.data

import android.content.Context
import android.util.Log
import com.millicast.AudioPlayback
import com.millicast.AudioTrack
import com.millicast.LayerData
import com.millicast.Media
import com.millicast.Subscriber
import com.millicast.VideoTrack
import com.millicast.devices.playback.AudioPlayback
import com.millicast.subscribers.state.LayerData
import io.dolby.rtscomponentkit.manager.SubscriptionManagerInterface
import io.dolby.rtscomponentkit.manager.TAG
import io.dolby.rtscomponentkit.utils.DispatcherProvider
import io.dolby.rtscomponentkit.utils.DispatcherProviderImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.webrtc.RTCStatsReport
import java.util.Optional

class RTSViewerDataStore constructor(
    context: Context,
    millicastSdk: MillicastSdk,
    dispatcherProvider: DispatcherProvider = DispatcherProviderImpl
) {
    private val apiScope = CoroutineScope(dispatcherProvider.default + Job())

    private val subscriptionDelegate = object : Subscriber.Listener {
        override fun onSubscribed() {
            apiScope.launch {
                _state.emit(State.Subscribed)
            }
        }

        override fun onSubscribedError(reason: String) {
            Log.d(TAG, "onSubscribedError: $reason")
            apiScope.launch {
                _state.emit(State.Error(SubscriptionError.SubscribeError(reason)))
            }
            _statistics.value = null
        }

        override fun onTrack(track: VideoTrack, p1: Optional<String>?) {
            Log.d(TAG, "onVideoTrack")
            apiScope.launch {
                _state.emit(State.VideoTrackReady(track))
            }
        }

        override fun onTrack(track: AudioTrack, p1: Optional<String>?) {
            Log.d(TAG, "onAudioTrack")
            apiScope.launch {
                _state.emit(State.AudioTrackReady(track))
            }
        }

        override fun onStatsReport(report: RTCStatsReport) {
            Log.d(TAG, "onStatsReport")
            _statistics.value = StatisticsData.from(report)
        }

        override fun onViewerCount(p0: Int) {
            Log.d("Subscriber", "onViewerCount")
        }

        override fun onConnected() {
            Log.d(TAG, "onConnected")
            startSubscribe()
        }

        override fun onActive(p0: String?, p1: Array<out String>?, p2: Optional<String>?) {
            Log.d(TAG, "onActive")
            apiScope.launch {
                _state.emit(State.StreamActive)
            }
        }

        override fun onInactive(p0: String?, p1: Optional<String>?) {
            Log.d(TAG, "onInactive")
            apiScope.launch {
                _state.emit(State.StreamInactive)
            }
        }

        override fun onStopped() {
            Log.d(TAG, "onStopped")
            apiScope.launch {
                _state.emit(State.StreamInactive)
            }
            _statistics.value = null
        }

        override fun onVad(p0: String?, p1: Optional<String>?) {
            Log.d(TAG, "onVad")
        }

        override fun onConnectionError(reason: String) {
            Log.d(TAG, "onConnectionError: $reason")
            _statistics.value = null
            apiScope.launch {
                _state.emit(State.Error(SubscriptionError.ConnectError(reason)))
            }
        }

        override fun onSignalingError(reason: String?) {
            Log.d(TAG, "onSignalingError: $reason")
            _statistics.value = null
        }

        override fun onLayers(mid: String?, activeLayers: Array<out LayerData>?, inactiveLayers: Array<out LayerData>?) {
            Log.d(TAG, "onLayers: $activeLayers")
            val filteredActiveLayers = activeLayers?.filter {
                // For H.264 there are no temporal layers and the id is set to 255. For VP8 use the first temporal layer.
                it.temporalLayerId == 0 || it.temporalLayerId == 255
            }

            filteredActiveLayers?.let { activeLayers ->
                val newActiveLayers = when (activeLayers?.count()) {
                    2 -> {
                        listOf(
                            StreamQualityType.Auto,
                            StreamQualityType.High(activeLayers[0]),
                            StreamQualityType.Low(activeLayers[1])
                        )
                    }
                    3 -> {
                        listOf(
                            StreamQualityType.Auto,
                            StreamQualityType.High(activeLayers[0]),
                            StreamQualityType.Medium(activeLayers[1]),
                            StreamQualityType.Low(activeLayers[2])
                        )
                    }
                    else -> emptyList()
                }

                if (_streamQualityTypes.value != newActiveLayers) {
                    _streamQualityTypes.value = newActiveLayers
                    // Update selected stream quality type everytime the `streamQualityTypes` change
                    // It preserves the current selected type if the new list has a stream matching the type `selectedStreamQualityType`
                    val updatedStreamQualityType = _streamQualityTypes.value.firstOrNull { type ->
                        _selectedStreamQualityType.value::class == type::class
                    } ?: StreamQualityType.Auto

                    _selectedStreamQualityType.value = updatedStreamQualityType
                }
            }
        }
    }

    private val subscriptionManager: SubscriptionManagerInterface =
        millicastSdk.initSubscriptionManager(subscriptionDelegate)

    private val _state: MutableSharedFlow<State> = MutableSharedFlow()
    val state: Flow<State> = _state.asSharedFlow()

    private val _statistics: MutableStateFlow<StatisticsData?> = MutableStateFlow(null)
    val statisticsData: Flow<StatisticsData?> = _statistics.asStateFlow()

    private var media: Media
    private var audioPlayback: ArrayList<AudioPlayback>? = null

    private var _streamQualityTypes: MutableStateFlow<List<StreamQualityType>> =
        MutableStateFlow(emptyList())
    val streamQualityTypes: Flow<List<StreamQualityType>> = _streamQualityTypes.asStateFlow()

    private var _selectedStreamQualityType: MutableStateFlow<StreamQualityType> =
        MutableStateFlow(StreamQualityType.Auto)
    val selectedStreamQualityType: Flow<StreamQualityType> =
        _selectedStreamQualityType.asStateFlow()

    init {
        millicastSdk.init(context)
        media = millicastSdk.getMedia(context)
        audioPlayback = media.audioPlayback
    }

    fun connect(streamName: String, accountId: String) = apiScope.launch {
        _state.emit(State.Connecting)
        subscriptionManager.connect(streamName, accountId)
    }

    private fun startSubscribe() = apiScope.launch {
        subscriptionManager.startSubscribe()
    }

    fun stopSubscribe() = apiScope.launch {
        subscriptionManager.stopSubscribe()

        resetStreamQualityTypes()
    }

    /**
     * Start the playback of selected audioPlayback if available.
     */
    fun audioPlaybackStart() {
        if (audioPlayback == null) {
            Log.d(TAG, "Creating new audioPlayback...")

            audioPlayback = media.audioPlayback
        } else {
            Log.d(TAG, "Using existing audioPlayback...")
        }
        Log.d(TAG, "AudioPlayback is: $audioPlayback")

        audioPlayback?.let {
            it[0].initPlayback()
            Log.d(TAG, "OK. Playback initiated.")
        }
    }

    fun selectStreamQualityType(type: StreamQualityType) = apiScope.launch {
        val success = subscriptionManager.selectLayer(type.layerData)
        if (success) {
            _selectedStreamQualityType.value = type
        }
    }

    private fun resetStreamQualityTypes() {
        _selectedStreamQualityType.value = StreamQualityType.Auto
        _streamQualityTypes.value = emptyList()
    }

    sealed class SubscriptionError(open val reason: String) {
        class SubscribeError(override val reason: String) : SubscriptionError(reason = reason)
        class ConnectError(override val reason: String) : SubscriptionError(reason = reason)
    }

    sealed class State {
        object Connecting : State()
        object Subscribed : State()
        object StreamActive : State()
        object StreamInactive : State()
        object Disconnected : State()
        class Error(val error: SubscriptionError) : State()
        class AudioTrackReady(val audioTrack: AudioTrack) : State()
        class VideoTrackReady(val videoTrack: VideoTrack) : State()
    }

    sealed class StreamQualityType {
        object Auto : StreamQualityType() {
            override fun equals(other: Any?): Boolean {
                return other is Auto
            }
        }
        data class High(val layer: LayerData) : StreamQualityType() {
            override fun equals(other: Any?): Boolean {
                return other is High && other.layer.isEqualTo(this.layer)
            }
        }
        data class Medium(val layer: LayerData) : StreamQualityType() {
            override fun equals(other: Any?): Boolean {
                return other is Medium && other.layer.isEqualTo(this.layer)
            }
        }
        data class Low(val layer: LayerData) : StreamQualityType() {
            override fun equals(other: Any?): Boolean {
                return other is Low && other.layer.isEqualTo(this.layer)
            }
        }

        val layerData: LayerData?
            get() = when (this) {
                is Auto -> null
                is High -> layer
                is Medium -> layer
                is Low -> layer
            }

        override fun equals(other: Any?): Boolean {
            return other is StreamQualityType && when (other) {
                is Auto -> (this as Auto) == other
                is High -> (this as High) == other
                is Medium -> (this as Medium) == other
                is Low -> (this as Low) == other
            }
        }
    }
}

fun LayerData.isEqualTo(other: LayerData): Boolean {
    return other.spatialLayerId == this.spatialLayerId &&
            other.temporalLayerId == this.temporalLayerId &&
            other.encodingId == this.encodingId &&
            other.maxSpatialLayerId == this.maxSpatialLayerId &&
            other.maxTemporalLayerId == this.maxTemporalLayerId
}
