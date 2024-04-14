package io.dolby.rtscomponentkit.data

import android.util.Log
import com.millicast.Core
import com.millicast.Media
import com.millicast.clients.ConnectionOptions
import com.millicast.devices.playback.AudioPlayback
import com.millicast.subscribers.Credential
import com.millicast.subscribers.Option
import com.millicast.subscribers.remote.RemoteAudioTrack
import com.millicast.subscribers.remote.RemoteVideoTrack
import com.millicast.subscribers.state.LayerData
import com.millicast.subscribers.state.LayerDataSelection
import io.dolby.rtscomponentkit.domain.MultiStreamStatisticsData
import io.dolby.rtscomponentkit.domain.StreamingData
import io.dolby.rtscomponentkit.utils.DispatcherProvider
import io.dolby.rtscomponentkit.utils.DispatcherProviderImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

class RTSViewerDataStore constructor(
    millicastSdk: MillicastSdk,
    dispatcherProvider: DispatcherProvider = DispatcherProviderImpl
) {
    private val apiScope = CoroutineScope(dispatcherProvider.default + Job())

    private val _state: MutableSharedFlow<State> = MutableSharedFlow()
    val state: Flow<State> = _state.asSharedFlow()

    private val _statistics: MutableStateFlow<MultiStreamStatisticsData?> = MutableStateFlow(null)
    val statisticsData: Flow<MultiStreamStatisticsData?> = _statistics.asStateFlow()

    private var media: Media
    private var audioPlayback: List<AudioPlayback>? = null

    private var _streamQualityTypes: MutableStateFlow<Map<String?, List<StreamQualityType>>> =
        MutableStateFlow(emptyMap())
    val streamQualityTypes: Flow<Map<String?, List<StreamQualityType>>> = _streamQualityTypes.asStateFlow()

    private var _selectedStreamQualityType: MutableStateFlow<StreamQualityType> =
        MutableStateFlow(StreamQualityType.Auto)
    val selectedStreamQualityType: Flow<StreamQualityType> =
        _selectedStreamQualityType.asStateFlow()

    private var listener: SingleStreamListener? = null

    init {
        media = millicastSdk.getMedia()
        audioPlayback = media.audioPlayback
    }

    suspend fun connect(streamName: String, accountId: String) {
        if (listener?.connected() == true) {
            return
        }

        _state.emit(State.Connecting)
        Core.initialize()

        val subscriber = Core.createSubscriber()

        subscriber.setCredentials(
            credential(
                subscriber.credentials,
                StreamingData(accountId = accountId, streamName = streamName)
            )
        )
        listener = SingleStreamListener(
            subscriber = subscriber,
            state = _state,
            statistics = _statistics,
            streamQualityTypes = _streamQualityTypes,
            selectedStreamQualityType = _selectedStreamQualityType
        ).apply { start() }

        subscriber.enableStats(true)

        try {
            subscriber.connect(ConnectionOptions(true))
            subscriber.subscribe(Option(statsDelayMs = 1_000))
        } catch (e: Exception) {
            Log.e(TAG, "${e.message}")
        }
    }

    private fun credential(
        credentials: Credential,
        streamingData: StreamingData
    ) = credentials.copy(
        streamName = streamingData.streamName,
        accountId = streamingData.accountId,
        apiUrl = "https://director.millicast.com/api/director/subscribe"
    )

    fun disconnect() {
        listener?.release()
        listener = null

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

    private fun resetStreamQualityTypes() {
        _selectedStreamQualityType.value = StreamQualityType.Auto
        _streamQualityTypes.value = emptyMap()
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
        object Disconnecting : State()
        object Disconnected : State()
        class Error(val error: SubscriptionError) : State()
        class AudioTrackReady(val audioTrack: RemoteAudioTrack) : State()
        class VideoTrackReady(val videoTrack: RemoteVideoTrack) : State()
    }

    sealed class StreamQualityType {
        object Auto : StreamQualityType() {
            override fun equals(other: Any?): Boolean {
                return other is Auto
            }
        }

        data class High(val layer: LayerDataSelection) : StreamQualityType() {
            override fun equals(other: Any?): Boolean {
                return other is High && other.layer == this.layer
            }
        }

        data class Medium(val layer: LayerDataSelection) : StreamQualityType() {
            override fun equals(other: Any?): Boolean {
                return other is Medium && other.layer == this.layer
            }
        }

        data class Low(val layer: LayerDataSelection) : StreamQualityType() {
            override fun equals(other: Any?): Boolean {
                return other is Low && other.layer == this.layer
            }
        }

        val layerData: LayerDataSelection?
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

    companion object {
        const val TAG = "io.dolby.rtscomponentkit"
    }
}

fun LayerData.isEqualTo(other: LayerData): Boolean {
    return other.spatialLayerId == this.spatialLayerId &&
        other.temporalLayerId == this.temporalLayerId &&
        other.encodingId == this.encodingId &&
        other.maxSpatialLayerId == this.maxSpatialLayerId &&
        other.maxTemporalLayerId == this.maxTemporalLayerId
}
