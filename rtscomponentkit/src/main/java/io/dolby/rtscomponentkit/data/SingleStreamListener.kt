package io.dolby.rtscomponentkit.data

import android.util.Log
import com.millicast.Subscriber
import com.millicast.clients.stats.RtsReport
import com.millicast.subscribers.ForcePlayoutDelay
import com.millicast.subscribers.Option
import com.millicast.subscribers.remote.RemoteAudioTrack
import com.millicast.subscribers.remote.RemoteVideoTrack
import com.millicast.subscribers.state.LayerDataSelection
import com.millicast.subscribers.state.SubscriberConnectionState
import io.dolby.rtscomponentkit.data.RTSViewerDataStore.Companion.TAG
import io.dolby.rtscomponentkit.data.multistream.safeLaunch
import io.dolby.rtscomponentkit.domain.MultiStreamStatisticsData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class SingleStreamListener(
    private val subscriber: Subscriber,
    private val state: MutableSharedFlow<RTSViewerDataStore.State>,
    private val statistics: MutableStateFlow<MultiStreamStatisticsData?>,
    private val streamQualityTypes: MutableStateFlow<Map<String?, List<RTSViewerDataStore.StreamQualityType>>>,
    private val selectedStreamQualityType: MutableStateFlow<RTSViewerDataStore.StreamQualityType>
) {
    private lateinit var coroutineScope: CoroutineScope
    private var subscriptionJob: Job? = null

    private fun <T> Flow<T>.collectInLocalScope(
        collector: FlowCollector<T>
    ) = this.let {
        coroutineScope.launch { it.collect(collector) }
    }

    fun start() {
        Log.d(TAG, "Listener start $this")
        coroutineScope = CoroutineScope(Dispatchers.IO)

        subscriber.state.map { it.connectionState }.distinctUntilChanged()
            .collectInLocalScope { state ->
                Log.d(TAG, "New state: $state")
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
                        onConnectionError("Disconnected")
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

        subscriber.state.map { it.viewers }.collectInLocalScope {
            onViewerCount(it)
        }

        subscriber.onRemoteTrack.collectInLocalScope { holder ->
            Log.d(TAG, "onTrack, $holder, ${holder.currentMid}")
            when (holder) {
                is RemoteVideoTrack -> {
                    onTrack(holder)
                    Log.d(TAG, "onVideoTrack ${holder.currentMid}, ${holder.isActive}")
                    holder.onState.collectInLocalScope { trackState ->
                        Log.d(TAG, "onVideoTrack state ${trackState.mid}, ${trackState.isActive}")
                        trackState.layers?.let { layers ->
                            onLayers(holder.currentMid, layers.activeLayers)
                        }
                        state.emit(
                            if (trackState.isActive) {
                                RTSViewerDataStore.State.StreamActive
                            } else {
                                RTSViewerDataStore.State.StreamInactive
                            }
                        )
                    }
                }

                is RemoteAudioTrack -> {
                    onTrack(holder)
                }
            }
        }

        subscriber.rtcStatsReport.collectInLocalScope { report ->
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

    private suspend fun onConnected() {
        startSubscription()
    }

    private fun onDisconnected() {
        // nothing
    }

    private fun startSubscription() {
        subscriptionJob?.cancel()
        subscriptionJob = CoroutineScope(Dispatchers.IO).safeLaunch(block = {
            Log.d(TAG, "Start Subscribing")
            subscriber.subscribe()
        })
    }
    fun release() {
        Log.d(TAG, "Release Millicast $this $subscriber")
        subscriptionJob?.cancel()
        subscriptionJob = null
        subscriber.disconnect()
        coroutineScope.cancel()
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

    private fun onTrack(track: RemoteVideoTrack) {
        Log.d(TAG, "onVideoTrack")
        coroutineScope.launch {
            state.emit(RTSViewerDataStore.State.VideoTrackReady(track))
        }
    }

    private fun onTrack(track: RemoteAudioTrack) {
        Log.d(TAG, "onAudioTrack")
        coroutineScope.launch {
            state.emit(RTSViewerDataStore.State.AudioTrackReady(track))
        }
    }

    private fun onStatsReport(report: RtsReport) {
        statistics.value = MultiStreamStatisticsData.from(report)
        Log.d(TAG, "onStatsReport, ${statistics.value}, $subscriber, $this")
    }

    private fun onViewerCount(p0: Int) {
        Log.d("Subscriber", "onViewerCount $p0")
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
                    RTSViewerDataStore.SubscriptionError.ConnectError(reason)
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
        activeLayers: List<LayerDataSelection>
    ) {
        Log.d(TAG, "onLayers: $mid, $activeLayers")
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

        if (streamQualityTypes.value[mid] != trackLayerDataList) {
            val mutable = streamQualityTypes.value.toMutableMap()
            mutable[mid] = trackLayerDataList
            streamQualityTypes.value = mutable
            // Update selected stream quality type everytime the `streamQualityTypes` change
            // It preserves the current selected type if the new list has a stream matching the type `selectedStreamQualityType`
            val updatedStreamQualityType = trackLayerDataList.firstOrNull { type ->
                selectedStreamQualityType.value::class == type::class
            } ?: trackLayerDataList.last()

            selectedStreamQualityType.value = updatedStreamQualityType
        }
    }
}
