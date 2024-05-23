package io.dolby.rtscomponentkit.data.multistream

import android.util.Log
import com.millicast.Subscriber
import com.millicast.clients.stats.RtsReport
import com.millicast.subscribers.remote.RemoteAudioTrack
import com.millicast.subscribers.remote.RemoteVideoTrack
import com.millicast.subscribers.state.LayerData
import com.millicast.subscribers.state.SubscriberConnectionState
import io.dolby.rtscomponentkit.domain.MultiStreamStatisticsData
import io.dolby.rtscomponentkit.domain.MultiStreamingData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MultiStreamListener(
    private val data: MutableStateFlow<MultiStreamingData>,
    private var subscriber: Subscriber
) {
    private lateinit var coroutineScope: CoroutineScope

    private fun <T> Flow<T>.collectInLocalScope(collector: FlowCollector<T>) =
        coroutineScope.launch { distinctUntilChanged().collect(collector) }

    fun start() {
        Log.d(TAG, "Listener start")
        coroutineScope = CoroutineScope(Dispatchers.IO)

        subscriber.state.map { it.connectionState }.collectInLocalScope { state ->
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
//                    onConnectionError(state.httpCode, state.reason)
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

//        subscriber.layers.collectInLocalScope {
//            onLayers(it.mid, it.activeLayers, it.inactiveLayersEncodingIds)
//        }

        subscriber.state.map { it.viewers }.collectInLocalScope {
            onViewerCount(it)
        }

        subscriber.onRemoteTrack.collectInLocalScope { remoteTrack ->
            when (remoteTrack) {
                is RemoteVideoTrack -> {
                    onVideoTrack(remoteTrack)
                }

                is RemoteAudioTrack -> {
                    onAudioTrack(remoteTrack)
                }
            }
        }

        subscriber.rtcStatsReport.collectInLocalScope { report ->
            onStatsReport(report)
        }

        subscriber.signalingError.collectInLocalScope {
            Log.d(TAG, "subscriber.signalingError: $it")
            onSignalingError(it)
        }

        subscriber.vad.collectInLocalScope {
            Log.d(TAG, "subscriber.vad: $it")
        }

        subscriber.onTransformableFrame = { ssrc: Int, timestamp: Int, data: ByteArray ->
            Log.d(TAG, "onFrameMetadata: $ssrc, $timestamp, ${data.size}")
        }
    }

    fun connected(): Boolean = subscriber.isSubscribed

    fun disconnect() {
        subscriber.disconnect()
        coroutineScope.coroutineContext.cancelChildren()
        coroutineScope.coroutineContext.cancel()
        Log.d(TAG, "Disconnecting subscriber...")
    }

    private fun onConnected() {
        Log.d(
            TAG,
            "onConnected, this: $this, thread: ${Thread.currentThread().id}"
        )
        data.update {
            it.copy(isConnected = true, error = null)
        }
    }

    private fun onDisconnected() {
        Log.d(TAG, "onDisconnected")
        data.update {
            it.populateError(error = "Disconnected")
        }
    }

    private fun onConnectionError(p0: Int, p1: String?) {
        Log.d(TAG, "onConnectionError: $p0, $p1")
        data.update {
            it.populateError(error = p1 ?: "Connection error")
        }
    }

    private fun onSignalingError(p0: String?) {
        Log.d(TAG, "onSignalingError: $p0")
        data.update {
            it.populateError(error = p0 ?: "Signaling error")
        }
    }

    private fun onStatsReport(p0: RtsReport?) {
        p0?.let { report ->
            data.update { data ->
                data.copy(
                    statisticsData = MultiStreamStatisticsData.from(
                        report
                    )
                )
            }
        }
    }

    private fun onViewerCount(p0: Int) {
        Log.d(TAG, "onViewerCount: $p0")
        data.update { data -> data.copy(viewerCount = p0) }
    }

    private fun onSubscribed() {
        Log.d(TAG, "onSubscribed")
        data.update { data -> data.copy(isSubscribed = true, isSubscribing = false, error = null) }
    }

    private fun onSubscribedError(p0: String?) {
        Log.d(TAG, "onSubscribedError: $p0")
        data.update {
            it.copy(error = p0 ?: "Subscribed error", isSubscribed = false, isSubscribing = false)
        }
    }

    private fun onVideoTrack(videoTrack: RemoteVideoTrack) {
        Log.d(TAG, "onVideoTrack: $videoTrack")
        data.update { data ->
            data.copy(
                videoTracks = subscriber.currentState.tracks
                    .filter { it is RemoteVideoTrack }
                    .map { it as RemoteVideoTrack }
            )
        }
    }

    private fun onAudioTrack(audioTrack: RemoteAudioTrack) {
        Log.d(TAG, "onAudioTrack: $audioTrack")
        data.update { data ->
            data.copy(
                audioTracks = subscriber.currentState.tracks
                    .filter { it is RemoteAudioTrack }
                    .map { it as RemoteAudioTrack }
            )
        }
    }

    private fun onStopped() {
        Log.d(TAG, "onStopped")
        data.update {
            it.copy(isSubscribed = false)
        }
    }

    private fun onLayers(
        mid: String,
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

        val trackLayerDataList = when (filteredActiveLayers.count()) {
            2 -> listOf(
                LowLevelVideoQuality.Auto(),
                LowLevelVideoQuality.High(filteredActiveLayers[0]),
                LowLevelVideoQuality.Low(filteredActiveLayers[1])
            )

            3 -> listOf(
                LowLevelVideoQuality.Auto(),
                LowLevelVideoQuality.High(filteredActiveLayers[0]),
                LowLevelVideoQuality.Medium(filteredActiveLayers[1]),
                LowLevelVideoQuality.Low(filteredActiveLayers[2])
            )

            else -> listOf(
                LowLevelVideoQuality.Auto()
            )
        }
        data.update {
            val mutableOldTrackLayerData = it.trackLayerData.toMutableMap()
            mutableOldTrackLayerData[mid] = trackLayerDataList
            it.copy(trackLayerData = mutableOldTrackLayerData.toMap())
        }
    }

    companion object {
        const val TAG = "io.dolby.interactiveplayer.listener"
    }
}
