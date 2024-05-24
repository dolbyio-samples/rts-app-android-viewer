package io.dolby.rtscomponentkit.data

import android.util.Log
import com.millicast.Subscriber
import com.millicast.clients.stats.RtsReport
import com.millicast.devices.track.AudioTrack
import com.millicast.devices.track.VideoTrack
import com.millicast.subscribers.Option
import com.millicast.subscribers.state.ActivityStream
import com.millicast.subscribers.state.LayerData
import com.millicast.subscribers.state.SubscriberConnectionState
import com.millicast.utils.MillicastException
import io.dolby.rtscomponentkit.data.RTSViewerDataStore.Companion.TAG
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.Optional

class SingleStreamListener(
    private val subscriber: Subscriber,
    private val state: MutableSharedFlow<RTSViewerDataStore.State>,
    private val statistics: MutableStateFlow<SingleStreamStatisticsData?>,
    private val streamQualityTypes: MutableStateFlow<List<RTSViewerDataStore.StreamQualityType>>,
    private val selectedStreamQualityType: MutableStateFlow<RTSViewerDataStore.StreamQualityType>
) {
    private lateinit var coroutineScope: CoroutineScope

    private fun <T> Flow<T>.collectInLocalScope(
        collector: FlowCollector<T>
    ) = this.let {
        coroutineScope.launch { it.collect(collector) }
    }

    fun start() {
        Log.d(TAG, "Listener start $this")
        coroutineScope = CoroutineScope(Dispatchers.IO)

        subscriber.state.map { it.connectionState }.distinctUntilChanged().collectInLocalScope { state ->
            when (state) {
                SubscriberConnectionState.Connected -> {
                    onConnected()
                }

                SubscriberConnectionState.Connecting -> {
                    // nothing
                }

                SubscriberConnectionState.Disconnected -> {
                    onDisconnected()
                }

                is SubscriberConnectionState.DisconnectedError -> {
                    onConnectionError(state.reason)
                }

                SubscriberConnectionState.Disconnecting -> {
                    // nothing
                }

                is SubscriberConnectionState.Error -> {
                    onSubscribedError(state.reason)
                }

                SubscriberConnectionState.Stopped -> {
                    onStopped()
                }

                SubscriberConnectionState.Subscribed -> {
                    onSubscribed()
                }
            }
        }

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

        subscriber.state.map { it.viewers }.collectInLocalScope {
            onViewerCount(it)
        }

        subscriber.tracks.collectInLocalScope { holder ->
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
            onSignalingError(it)
        }

        subscriber.vad.collectInLocalScope {
            Log.d(TAG, "subscriber.vad: $it")
        }
    }

    fun connected(): Boolean = subscriber.isSubscribed

    private fun onDisconnected() {
        // nothing
    }

    fun release() {
        Log.d(TAG, "Release Millicast $this $subscriber")
        subscriber.release()
        coroutineScope.cancel()
    }

    suspend fun selectLayer(layer: LayerData?): Boolean {
        return try {
            subscriber.select(layer)
            true
        } catch (e: MillicastException) {
            false
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
        statistics.value = SingleStreamStatisticsData.from(report)
        Log.d(TAG, "onStatsReport, ${statistics.value}, $subscriber, $this")
    }

    private fun onViewerCount(p0: Int) {
        Log.d("Subscriber", "onViewerCount $p0")
    }

    private suspend fun onConnected() {
        Log.d(TAG, "onConnected")
        try {
            subscriber.subscribe(Option(statsDelayMs = 1_000))
        } catch (e: MillicastException) {
            e.printStackTrace()
        }
    }

    private fun onActive(p0: String?, p1: Array<out String>, p2: String?) {
        Log.d(TAG, "onActive")
        coroutineScope.launch {
            state.emit(RTSViewerDataStore.State.StreamActive)
        }
    }

    private fun onInactive(p0: String?, p1: String?) {
        Log.d(TAG, "onInactive")
        onConnectionError("Stream Inactive")
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

    private fun onConnectionError(reason: String) {
        Log.d(TAG, "onConnectionError: $this $subscriber")
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

    private fun onSignalingError(reason: String) {
        Log.d(TAG, "onSignalingError: $reason")
        statistics.value = null
    }

    private fun onLayers(
        mid: String?,
        activeLayers: Array<out LayerData>,
        inactiveLayers: Array<String>
    ) {
        Log.d(
            TAG,
            "onLayers: $mid, ${activeLayers.contentToString()}, ${
            inactiveLayers.contentToString()
            }"
        )
        val filteredActiveLayers = mutableListOf<LayerData>()
        var simulcastLayers = activeLayers.filter { it.encodingId.isNotEmpty() }
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

        filteredActiveLayers.sortWith(object : Comparator<LayerData> {
            override fun compare(o1: LayerData?, o2: LayerData?): Int {
                if (o1 == null) return -1
                if (o2 == null) return 1
                return when (o2.encodingId.lowercase()) {
                    "h" -> -1
                    "l" -> if (o1.encodingId.lowercase() == "h") -1 else 1
                    "m" -> if (o1.encodingId.lowercase() == "h" || o1.encodingId.lowercase() != "l") -1 else 1
                    else -> 1
                }
            }
        })

        val trackLayerDataList = when {
            filteredActiveLayers.count() == 2 -> listOf(
                RTSViewerDataStore.StreamQualityType.Auto,
                RTSViewerDataStore.StreamQualityType.High(filteredActiveLayers[0]),
                RTSViewerDataStore.StreamQualityType.Low(filteredActiveLayers[1])
            )

            filteredActiveLayers.count() >= 3 -> listOf(
                RTSViewerDataStore.StreamQualityType.Auto,
                RTSViewerDataStore.StreamQualityType.High(filteredActiveLayers[0]),
                RTSViewerDataStore.StreamQualityType.Medium(filteredActiveLayers[1]),
                RTSViewerDataStore.StreamQualityType.Low(filteredActiveLayers[2])
            )

            else -> listOf(
                RTSViewerDataStore.StreamQualityType.Auto
            )
        }

        if (streamQualityTypes.value != trackLayerDataList) {
            streamQualityTypes.value = trackLayerDataList
            // Update selected stream quality type everytime the `streamQualityTypes` change
            // It preserves the current selected type if the new list has a stream matching the type `selectedStreamQualityType`
            val updatedStreamQualityType = streamQualityTypes.value.firstOrNull { type ->
                selectedStreamQualityType.value::class == type::class
            } ?: trackLayerDataList.last()

            coroutineScope.launch { selectLayer(updatedStreamQualityType.layerData) }

            selectedStreamQualityType.value = updatedStreamQualityType
        }
    }
}
