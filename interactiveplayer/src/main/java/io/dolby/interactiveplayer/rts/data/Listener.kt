package io.dolby.interactiveplayer.rts.data

import android.util.Log
import com.millicast.Subscriber
import com.millicast.clients.state.ConnectionState
import com.millicast.clients.stats.RtsReport
import com.millicast.devices.track.AudioTrack
import com.millicast.devices.track.TrackType
import com.millicast.devices.track.VideoTrack
import com.millicast.subscribers.Option
import com.millicast.subscribers.ProjectionData
import com.millicast.subscribers.state.ActivityStream
import com.millicast.subscribers.state.LayerData
import com.millicast.subscribers.state.SubscriptionState
import com.millicast.utils.MillicastOriginalCallingException
import io.dolby.interactiveplayer.rts.data.MultiStreamingRepository.Companion.TAG
import io.dolby.interactiveplayer.rts.domain.MultiStreamStatisticsData
import io.dolby.interactiveplayer.rts.domain.MultiStreamingData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch
import java.util.Optional
import kotlin.jvm.optionals.getOrNull

class Listener(
    private val data: MutableStateFlow<MultiStreamingData>,
    private var subscriber: Subscriber,
    private val options: Option
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
                    onConnectionError(state.httpCode, state.reason)
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
                    onVideoTrack(holder.track as VideoTrack, Optional.ofNullable(holder.mid))
                }

                is AudioTrack -> {
                    onAudioTrack(holder.track as AudioTrack, Optional.ofNullable(holder.mid))
                }
            }
        }

        subscriber.rtcStatsReport.collectInLocalScope { report ->
            //TODO: update the report structure
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

    suspend fun disconnect() {
        Log.d(TAG, "Cancelling coroutines...")
        coroutineScope.coroutineContext.cancelChildren()
        coroutineScope.coroutineContext.cancel()
        Log.d(TAG, "Disconnecting subscriber...")
        subscriber.disconnect()
    }

    private suspend fun onConnected() {
        Log.d(
            TAG,
            "onConnected, this: $this, thread: ${Thread.currentThread().id}"
        )

        subscriber.subscribe(options = options)
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
            it.populateError(error = p1 ?: "Unknown error")
        }
    }

    private fun onSignalingError(p0: String?) {
        Log.d(TAG, "onSignalingError: $p0")
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

    private fun onVideoTrack(p0: VideoTrack, p1: Optional<String>) {
        val mid = p1.getOrNull()
        Log.d(TAG, "onVideoTrack: $mid, $p0")
        data.update { data ->
            if (data.videoTracks.isEmpty()) {
                data.addPendingMainVideoTrack(p0, mid)
            } else {
                data.getPendingVideoTrackInfoOrNull()?.let { trackInfo ->
                    data.appendOtherVideoTrack(trackInfo, p0, mid, trackInfo.sourceId)
                } ?: data
            }
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

    private fun onFrameMetadata(p0: Int, p1: Int, p2: ByteArray?) {
        Log.d(TAG, "onFrameMetadata: $p0, $p1")
        TODO("Not yet implemented")
    }

    private suspend fun onActive(
        stream: String,
        tracksInfo: Array<out String>,
        sourceId: String?
    ) {
        Log.d(
            TAG,
            "onActive: $stream, ${tracksInfo.toList()}, $sourceId"
        )
        val pendingTracks = MultiStreamingData.parseTracksInfo(tracksInfo, sourceId)
        if (data.value.videoTracks.isEmpty()) {
            data.update { data ->
                data.addPendingMainVideoTrack(pendingTracks.videoTracks.firstOrNull())
            }
        } else {
            val newData = data.updateAndGet { data ->
                data.addPendingTracks(
                    pendingTracks,
                    processAudio = false
                )
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

    private fun onInactive(p0: String, p1: String?) {
        Log.d(TAG, "onInactive $p0, $p1")
        val sourceId = p1
        data.update { data ->
            data.videoTracks.filter { it.sourceId == sourceId }
                .forEach { it.videoTrack.removeRenderer() }
            val inactiveVideo = data.videoTracks.firstOrNull { it.sourceId == sourceId }
            var selectedVideoTrack = data.selectedVideoTrackId
            val tempVideos = data.videoTracks.toMutableList()
            inactiveVideo?.let {
                tempVideos.remove(inactiveVideo)
                if (data.selectedVideoTrackId == sourceId && tempVideos.isNotEmpty()) {
                    selectedVideoTrack = tempVideos[0].sourceId
                }
            }
            val inactiveAudio = data.audioTracks.firstOrNull { it.sourceId == sourceId }
            val tempAudios = data.audioTracks.toMutableList()
            inactiveAudio?.let {
                tempAudios.remove(inactiveAudio)
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
    }

    private fun onVad(p0: String?, p1: Optional<String>?) {
        TODO("Not yet implemented")
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
        mid?.let {
            val filteredActiveLayers =
                activeLayers.filter { it.temporalLayerId == 0 || it.temporalLayerId == 0xff }
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
    }

    suspend fun playVideo(
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
            try {
                subscriber.project(video.sourceId ?: "", arrayListOf(projectionData))
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
            } catch (e: MillicastOriginalCallingException) {
                e.printStackTrace()
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

    suspend fun stopVideo(video: MultiStreamingData.Video) {
        subscriber.unproject(arrayListOf(video.id))
        data.update {
            val mutableOldProjectedData = it.trackProjectedData.toMutableMap()
            mutableOldProjectedData.remove(video.id)
            it.copy(trackProjectedData = mutableOldProjectedData.toMap())
        }
    }

    suspend fun playAudio(audioTrack: MultiStreamingData.Audio) {
        val audioTrackIds = ArrayList(data.value.allAudioTrackIds)
        subscriber.unproject(audioTrackIds)

        val projectionData = ProjectionData(
            mid = audioTrack.id!!,
            trackId = MultiStreamingData.audio,
            media = MultiStreamingData.audio
        )
        subscriber.project(audioTrack.sourceId ?: "", arrayListOf(projectionData))
        data.update {
            it.copy(selectedAudioTrackId = audioTrack.sourceId)
        }
    }

    private suspend fun processPendingTracks(
        data: MultiStreamingData,
        processVideo: Boolean = true,
        processAudio: Boolean = true
    ) {
        if (data.isSubscribed) {
            if (processVideo) {
                val pendingVideoTracks = data.pendingVideoTracks.count { !it.added }
                repeat(pendingVideoTracks) {
                    subscriber.addRemoteTrack(TrackType.Video)
                }
            }
            if (processAudio) {
                val pendingAudioTracks = data.pendingAudioTracks.count { !it.added }
                repeat(pendingAudioTracks) {
                    subscriber.addRemoteTrack(TrackType.Audio)
                }
            }
            this.data.update {
                it.markPendingTracksAsAdded(
                    processVideo = processVideo,
                    processAudio = processAudio
                )
            }
        }
    }
}