package io.dolby.rtscomponentkit.data.multistream

import android.util.Log
import com.millicast.Subscriber
import com.millicast.clients.state.ConnectionState
import com.millicast.clients.stats.RtsReport
import com.millicast.devices.track.AudioTrack
import com.millicast.devices.track.TrackType
import com.millicast.devices.track.VideoTrack
import com.millicast.subscribers.ProjectionData
import com.millicast.subscribers.state.ActivityStream
import com.millicast.subscribers.state.LayerData
import com.millicast.subscribers.state.SubscriptionState
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
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.Optional
import kotlin.jvm.optionals.getOrNull

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
                    onConnectionError(state.httpCode, state.reason)
                }

                ConnectionState.Disconnecting -> {
                    // nothing
                }
            }
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

        subscriber.track.collectInLocalScope { holder ->
            when (holder.track) {
                is VideoTrack -> {
                    onVideoTrack(holder.track as VideoTrack, Optional.ofNullable(holder.mid))
                }

                is AudioTrack -> {
                    onAudioTrack(holder.track as AudioTrack, Optional.ofNullable(holder.mid))
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
        Log.d(TAG, "Cancelling coroutines...")
        coroutineScope.coroutineContext.cancelChildren()
        coroutineScope.coroutineContext.cancel()
        Log.d(TAG, "Disconnecting subscriber...")
        runBlocking { subscriber.disconnect() }
    }

    private fun onConnected() {
        Log.d(
            TAG,
            "onConnected, this: $this, thread: ${Thread.currentThread().id}"
        )
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

    private suspend fun onSubscribed() {
        Log.d(TAG, "onSubscribed")
        val newData =
            data.updateAndGet { data -> data.copy(isSubscribed = true, error = null) }
        processPendingTracks(newData)
    }

    private fun onSubscribedError(p0: String?) {
        Log.d(TAG, "onSubscribedError: $p0")
        data.update {
            it.copy(error = p0 ?: "Subscribed error")
        }
    }

    private fun onVideoTrack(videoTrack: VideoTrack, p1: Optional<String>) {
        val mid = p1.getOrNull()
        Log.d(TAG, "onVideoTrack: $mid, $videoTrack")
        data.update { data ->
            data.addVideoTrack(videoTrack, mid)
        }
    }

    private fun onAudioTrack(p0: AudioTrack, p1: Optional<String>) {
        val mid = p1.getOrNull()
        Log.d(TAG, "onAudioTrack: $mid, $p0")
        data.update { data ->
            if (data.audioTracks.isEmpty()) {
                data.addPendingMainAudioTrack(p0, mid)
            } else {
                data.getPendingAudioTrackInfoOrNull()?.let { trackInfo ->
                    data.appendAudioTrack(trackInfo, p0, p1.getOrNull(), trackInfo.sourceId)
                } ?: data
            }
        }
    }

    private suspend fun onActive(
        stream: String,
        tracksInfo: Array<out String>,
        sourceId: String?
    ) {
        Log.d(TAG, "onActive: $stream, ${tracksInfo.toList()}, $sourceId")

        val activeAudio = data.value.audioTracks.firstOrNull { it.sourceId == sourceId }
        val activeVideo = data.value.videoTracks.firstOrNull { it.sourceId == sourceId }
        val tempVideos = data.value.videoTracks.toMutableList()
        var reactivated = false
        activeVideo?.let {
            val toActivate = activeVideo.copy(active = true)
            tempVideos.remove(activeVideo)
            tempVideos.add(toActivate)
            data.update { data -> data.copy(videoTracks = tempVideos) }
            reactivated = true
        }
        val tempAudios = data.value.audioTracks.toMutableList()
        activeAudio?.let {
            val toActivate = activeAudio.copy(active = true)
            tempAudios.remove(activeAudio)
            tempAudios.add(toActivate)
            data.update { data -> data.copy(audioTracks = tempAudios) }
            reactivated = true
        }
        if (reactivated) return

        val pendingTracks = MultiStreamingData.parseTracksInfo(tracksInfo, sourceId)
        if (pendingTracks.videoTracks.isNotEmpty()) {
            val newData = data.updateAndGet { data ->
                data.addVideoTrack(pendingTracks.videoTracks)
            }
            processPendingTracks(newData, processAudio = false)
        }
        if (data.value.audioTracks.isEmpty()) {
            data.update { data ->
                data.addPendingMainAudioTrack(pendingTracks.audioTracks.firstOrNull())
            }
        } else {
            val newData = data.updateAndGet { data ->
                data.addPendingTracks(
                    pendingTracks,
                    processVideo = false
                )
            }
            processPendingTracks(newData, processVideo = false)
        }
    }

    private fun onInactive(p0: String, sourceId: String?) {
        Log.d(TAG, "onInactive $p0, $sourceId")
        data.update { data ->
            data.videoTracks.filter { it.sourceId == sourceId }
                .forEach { it.videoTrack.removeVideoSink() }
            val inactiveVideo = data.videoTracks.firstOrNull { it.sourceId == sourceId }
            var selectedVideoTrack = data.selectedVideoTrackId
            val tempVideos = data.videoTracks.toMutableList()
            inactiveVideo?.let {
                val toInactivate = inactiveVideo.copy(active = false)
                tempVideos.remove(inactiveVideo)
                tempVideos.add(toInactivate)

                if (data.selectedVideoTrackId == sourceId && tempVideos.isNotEmpty()) {
                    selectedVideoTrack = tempVideos[0].sourceId
                }
            }
            val inactiveAudio = data.audioTracks.firstOrNull { it.sourceId == sourceId }
            val tempAudios = data.audioTracks.toMutableList()
            inactiveAudio?.let {
                val toInactivate = inactiveAudio.copy(active = false)
                tempAudios.remove(inactiveAudio)
                tempAudios.add(toInactivate)
            }

            return@update data.copy(
                selectedVideoTrackId = selectedVideoTrack,
                videoTracks = tempVideos.toList(),
                audioTracks = tempAudios.toList()
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

    fun playVideo(
        video: MultiStreamingData.Video,
        preferredVideoQuality: VideoQuality
    ) {
        val availablePreferredVideoQuality =
            availablePreferredVideoQuality(video, preferredVideoQuality)
        val projected = data.value.trackProjectedData[video.id]
        if (projected == null || projected.videoQuality != availablePreferredVideoQuality?.videoQuality()) {
            Log.d(
                TAG,
                "project video ${video.id}, quality = $availablePreferredVideoQuality"
            )
            val projectionData =
                createProjectionData(
                    video,
                    availablePreferredVideoQuality
                )
            CoroutineScope(Dispatchers.IO).launch {
                subscriber.project(video.sourceId ?: "", arrayListOf(projectionData))
            }
            data.update {
                val mutableOldProjectedData = it.trackProjectedData.toMutableMap()
                mutableOldProjectedData[projectionData.mid] =
                    projectedDataFrom(
                        video.sourceId,
                        availablePreferredVideoQuality?.videoQuality()
                            ?: VideoQuality.AUTO,
                        projectionData.mid
                    )
                it.copy(trackProjectedData = mutableOldProjectedData.toMap())
            }
        }
    }

    private fun availablePreferredVideoQuality(
        video: MultiStreamingData.Video,
        preferredVideoQuality: VideoQuality
    ): LowLevelVideoQuality? {
        return data.value.trackLayerData[video.id]?.find {
            it.videoQuality() == preferredVideoQuality
        }
            ?: if (preferredVideoQuality != VideoQuality.AUTO) availablePreferredVideoQuality(
                video,
                preferredVideoQuality.lower()
            ) else {
                null
            }
    }

    fun stopVideo(video: MultiStreamingData.Video) {
        CoroutineScope(Dispatchers.IO).safeLaunch({
            subscriber.unproject(arrayListOf(video.id))
        })
        data.update {
            val mutableOldProjectedData = it.trackProjectedData.toMutableMap()
            mutableOldProjectedData.remove(video.id)
            it.copy(trackProjectedData = mutableOldProjectedData.toMap())
        }
    }

    fun playAudio(audioTrack: MultiStreamingData.Audio) {
        val audioTrackIds = ArrayList(data.value.allAudioTrackIds)
        CoroutineScope(Dispatchers.Main).safeLaunch({ subscriber.unproject(audioTrackIds) })

        val projectionData = ProjectionData(
            mid = audioTrack.id!!,
            trackId = MultiStreamingData.audio,
            media = MultiStreamingData.audio
        )
        CoroutineScope(Dispatchers.Main).safeLaunch({
            subscriber.project(
                audioTrack.sourceId ?: "",
                arrayListOf(projectionData)
            )
        })
        data.update {
            it.copy(selectedAudioTrackId = audioTrack.sourceId)
        }
    }

    private suspend fun processPendingTracks(
        multiStreamingData: MultiStreamingData,
        processVideo: Boolean = true,
        processAudio: Boolean = true
    ) {
        coroutineScope.safeLaunch({
            if (processVideo) {
                val pendingVideoTracks = multiStreamingData.pendingVideoTracks.count { !it.added }
                repeat(pendingVideoTracks) {
                    subscriber.addRemoteTrack(TrackType.Video)
                }
            }
            if (processAudio) {
                val pendingAudioTracks = multiStreamingData.pendingAudioTracks.count { !it.added }
                repeat(pendingAudioTracks) {
                    subscriber.addRemoteTrack(TrackType.Audio)
                }
            }
            data.update {
                it.markPendingTracksAsAdded(
                    processVideo = processVideo,
                    processAudio = processAudio
                )
            }
        })
    }

    companion object {
        const val TAG = "io.dolby.interactiveplayer.listener"
        private fun createProjectionData(
            video: MultiStreamingData.Video,
            availablePreferredVideoQuality: LowLevelVideoQuality?
        ) = ProjectionData(
            mid = video.id ?: "",
            trackId = video.trackId,
            media = video.mediaType,
            layer = availablePreferredVideoQuality?.layerData
        )

        private fun projectedDataFrom(
            sourceId: String?,
            videoQuality: VideoQuality,
            mid: String
        ) = MultiStreamingData.ProjectedData(
            mid = mid,
            sourceId = sourceId,
            videoQuality = videoQuality
        )
    }
}
