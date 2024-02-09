package io.dolby.rtscomponentkit.data

import android.util.Log
import com.millicast.Subscriber
import com.millicast.clients.state.ConnectionState
import com.millicast.clients.stats.RtsReport
import com.millicast.devices.track.AudioTrack
import com.millicast.devices.track.VideoTrack
import com.millicast.subscribers.state.ActivityStream
import com.millicast.subscribers.state.LayerData
import com.millicast.subscribers.state.SubscriptionState
import com.millicast.utils.MillicastException
import io.dolby.rtscomponentkit.data.RTSViewerDataStore.Companion.TAG
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.Optional

class Listener(
    private val subscriber: Subscriber,
    private val state: MutableSharedFlow<RTSViewerDataStore.State>,
    private val statistics: MutableStateFlow<StatisticsData?>,
    private val streamQualityTypes: MutableStateFlow<List<RTSViewerDataStore.StreamQualityType>>,
    private val selectedStreamQualityType: MutableStateFlow<RTSViewerDataStore.StreamQualityType>
) {
    private var coroutineScope = CoroutineScope(Dispatchers.IO)

    private fun <T> Flow<T>.collectInLocalScope(
        collector: FlowCollector<T>
    ) = this.let {
        coroutineScope.launch { it.collect(collector) }
    }

    fun start() {
        Log.d(TAG, "Listener start")
        coroutineScope = CoroutineScope(Dispatchers.IO)

        subscriber.onTransformableFrame = { ssrc: Int, timestamp: Int, data: ByteArray ->
            Log.d(TAG, "onFrameMetadata: $ssrc, $timestamp, ${data.size}")
        }

        subscriber.activity.collectInLocalScope {
            when (it) {
                is ActivityStream.Active -> onActive(
                    it.streamId,
                    it.track,
                    it.sourceId
                )

                is ActivityStream.Inactive -> onInactive(
                    it.streamId,
                    it.sourceId
                )
            }
        }

        subscriber.layers.collectInLocalScope {
            onLayers(it.mid, it.activeLayers, it.inactiveLayersEncodingIds)
        }

        subscriber.state.map { it.subscriptionState }.collectInLocalScope { state ->
            when (state) {
                SubscriptionState.Default -> {}
                is SubscriptionState.Error -> {
                    onSubscribedError(state.reason)
                }

                SubscriptionState.Stopped -> {
                    onStopped()
                }

                SubscriptionState.Subscribed -> {
                    onSubscribed()
                }
            }
        }

        subscriber.state.map { it.viewers }.collectInLocalScope {
            onViewerCount(it)
        }

        subscriber.state.map { it.connectionState }.collectInLocalScope { state ->
            when (state) {
                ConnectionState.Default -> {}
                ConnectionState.Connected -> {
                    onConnected()
                }

                ConnectionState.Connecting -> {
                    // nothing
                }

                ConnectionState.Disconnected -> {
                    onDisconnected()
                }

                is ConnectionState.DisconnectedError -> {
                    onConnectionError(state.reason)
                }

                ConnectionState.Disconnecting -> {
                    // nothing
                }
            }
        }

        subscriber.track.collectInLocalScope { holder ->
            Log.d(TAG, "onTrack, ${holder.track}, ${holder.mid}")
            when (holder.track) {
                is VideoTrack -> {
                    onTrack(holder.track as VideoTrack, Optional.ofNullable(holder.mid))
                }

                is AudioTrack -> {
                    onTrack(holder.track as AudioTrack, Optional.ofNullable(holder.mid))
                }
            }
        }

        subscriber.rtcStatsReport.collectInLocalScope { report ->
            // TODO: update the report structure
            onStatsReport(report)
        }

        subscriber.signalingError.collectInLocalScope {
            Log.d(TAG, "subscriber.signalingError: $it")
        }

        subscriber.vad.collectInLocalScope {
            Log.d(TAG, "subscriber.vad: $it")
        }
    }

    fun connected(): Boolean = subscriber.isSubscribed

    private fun onDisconnected() {
        // nothing
    }

    private suspend fun startSubscribe(): Boolean {
        // Subscribe to Millicast
        var success = true
        try {
            subscriber.subscribe()
        } catch (e: Exception) {
            success = false
            Log.d(TAG, "${e.message}")
        }
        enableStatsSub(10000)
        return success
    }

    suspend fun stopSubscribe(): Boolean {
        // Stop subscribing to Millicast.
        var success = true
        try {
            subscriber.unsubscribe()
        } catch (e: Exception) {
            success = false
            Log.d(TAG, "${e.message}")
        }

        // Disconnect from Millicast.
        try {
            subscriber.disconnect()
        } catch (e: java.lang.Exception) {
            success = false
            Log.d(TAG, "${e.message}")
        }
        enableStatsSub(0)
        return success
    }

    private suspend fun enableStatsSub(enable: Int) {
        subscriber.let {
            try {
                if (enable > 0) {
                    it.enableStats(true)
                    Log.d(TAG, "YES. Interval: $enable ms.")
                } else {
                    it.enableStats(false)
                    Log.d(TAG, "NO.")
                }
            } catch (e: IllegalStateException) {
                Log.e(TAG, e.message ?: e.toString())
            }
        }
    }

    suspend fun selectLayer(layer: LayerData?): Boolean {
        try {
            subscriber.select(layer)
            return true
        } catch (e: MillicastException) {
            return false
        }
    }

    private fun onSubscribed() {
        coroutineScope.launch {
            state.emit(RTSViewerDataStore.State.Subscribed)
        }
    }

    private fun onSubscribedError(reason: String) {
        Log.d(TAG, "onSubscribedError: $reason")
        coroutineScope.launch {
            state.emit(
                RTSViewerDataStore.State.Error(
                    RTSViewerDataStore.SubscriptionError.SubscribeError(
                        reason
                    )
                )
            )
        }
        statistics.value = null
    }

    private fun onTrack(track: VideoTrack, p1: Optional<String>?) {
        Log.d(TAG, "onVideoTrack")
        coroutineScope.launch {
            state.emit(RTSViewerDataStore.State.VideoTrackReady(track))
        }
    }

    private fun onTrack(track: AudioTrack, p1: Optional<String>?) {
        Log.d(TAG, "onAudioTrack")
        coroutineScope.launch {
            state.emit(RTSViewerDataStore.State.AudioTrackReady(track))
        }
    }

    private fun onStatsReport(report: RtsReport) {
        Log.d(TAG, "onStatsReport")
        statistics.value = StatisticsData.from(report)
    }

    private fun onViewerCount(p0: Int) {
        Log.d("Subscriber", "onViewerCount")
    }

    private suspend fun onConnected() {
        Log.d(TAG, "onConnected")
        startSubscribe()
    }

    private fun onActive(p0: String?, p1: Array<out String>?, p2: String?) {
        Log.d(TAG, "onActive")
        coroutineScope.launch {
            state.emit(RTSViewerDataStore.State.StreamActive)
        }
    }

    private fun onInactive(p0: String?, p1: String?) {
        Log.d(TAG, "onInactive")
        coroutineScope.launch {
            state.emit(RTSViewerDataStore.State.StreamInactive)
        }
    }

    private fun onStopped() {
        Log.d(TAG, "onStopped")
        coroutineScope.launch {
            state.emit(RTSViewerDataStore.State.StreamInactive)
        }
        statistics.value = null
    }

    private fun onVad(p0: String?, p1: Optional<String>?) {
        Log.d(TAG, "onVad")
    }

    private fun onConnectionError(reason: String) {
        Log.d(TAG, "onConnectionError: $reason")
        statistics.value = null
        coroutineScope.launch {
            state.emit(
                RTSViewerDataStore.State.Error(
                    RTSViewerDataStore.SubscriptionError.ConnectError(
                        reason
                    )
                )
            )
        }
    }

    private fun onSignalingError(reason: String?) {
        Log.d(TAG, "onSignalingError: $reason")
        statistics.value = null
    }

    private fun onLayers(
        mid: String?,
        activeLayers: Array<out LayerData>?,
        inactiveLayers: Array<String>?
    ) {
        Log.d(TAG, "onLayers: $activeLayers")
        val filteredActiveLayers = activeLayers?.filter {
            // For H.264 there are no temporal layers and the id is set to 255. For VP8 use the first temporal layer.
            it.temporalLayerId == 0 || it.temporalLayerId == 255
        }

        filteredActiveLayers?.let { activeLayers ->
            val newActiveLayers = when (activeLayers?.count()) {
                2 -> {
                    listOf(
                        RTSViewerDataStore.StreamQualityType.Auto,
                        RTSViewerDataStore.StreamQualityType.High(activeLayers[0]),
                        RTSViewerDataStore.StreamQualityType.Low(activeLayers[1])
                    )
                }

                3 -> {
                    listOf(
                        RTSViewerDataStore.StreamQualityType.Auto,
                        RTSViewerDataStore.StreamQualityType.High(activeLayers[0]),
                        RTSViewerDataStore.StreamQualityType.Medium(activeLayers[1]),
                        RTSViewerDataStore.StreamQualityType.Low(activeLayers[2])
                    )
                }

                else -> emptyList()
            }

            if (streamQualityTypes.value != newActiveLayers) {
                streamQualityTypes.value = newActiveLayers
                // Update selected stream quality type everytime the `streamQualityTypes` change
                // It preserves the current selected type if the new list has a stream matching the type `selectedStreamQualityType`
                val updatedStreamQualityType = streamQualityTypes.value.firstOrNull { type ->
                    selectedStreamQualityType.value::class == type::class
                } ?: RTSViewerDataStore.StreamQualityType.Auto

                selectedStreamQualityType.value = updatedStreamQualityType
            }
        }
    }
}
