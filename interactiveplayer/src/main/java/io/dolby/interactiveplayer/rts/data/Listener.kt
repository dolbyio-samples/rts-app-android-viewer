package io.dolby.interactiveplayer.rts.data

import android.util.Log
import com.millicast.Subscriber
import com.millicast.clients.state.ConnectionState
import com.millicast.devices.track.TrackType
import com.millicast.subscribers.ProjectionData
import com.millicast.subscribers.state.ActivityStream
import com.millicast.subscribers.state.LayerData
import com.millicast.subscribers.state.SubscriptionState
import io.dolby.interactiveplayer.rts.domain.MultiStreamingData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch

class Listener(
    private val TAG: String,
    private val data: MutableStateFlow<MultiStreamingData>,
    var subscriber: Subscriber
) {
    private var coroutineScope = CoroutineScope(Dispatchers.IO)
    private var flowJob: Job? = null

    private suspend fun <T> Flow<T>.collect(
        coroutineScope: CoroutineScope,
        collector: FlowCollector<T>
    ) = this.let {
        coroutineScope.launch { it.collect(collector) }
    }

    fun start() {
        flowJob = coroutineScope.launch {
            // added it, even tho it's not used currently, it shows how it can be registered
            subscriber.onTransformableFrame = { ssrc: Int, timestamp: Int, data: ByteArray ->
                Log.d(TAG, "onFrameMetadata: $ssrc, $timestamp, ${data.size}")
            }

            subscriber.activity.collect(this) {
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

            subscriber.layers.collect(this) {
                onLayers(it.mid, it.activeLayers, it.inactiveLayersEncodingIds)
            }

            subscriber.state.map { it.subscriptionState }.collect(this) { state ->
                when (state) {
                    SubscriptionState.Default -> {}
                    is SubscriptionState.Error -> {
                        Log.d(TAG, "onSubscribedError: ${state.reason}")
                        data.update {
                            it.copy(error = state.reason)
                        }
                    }

                    SubscriptionState.Stopped -> {
                        Log.d(TAG, "onStopped")
                    }

                    SubscriptionState.Subscribed -> {
                        Log.d(TAG, "onSubscribed")
                        val newData =
                            data.updateAndGet { data ->
                                data.copy(
                                    isSubscribed = true,
                                    error = null
                                )
                            }
                        processPendingTracks(newData)
                    }
                }
            }

            subscriber.state.map { it.viewers }.collect(this) {
                Log.d(TAG, "onViewerCount: $it")
                data.update { data -> data.copy(viewerCount = it) }
            }

            subscriber.state.map { it.connectionState }.collect(this) { state ->
                when (state) {
                    ConnectionState.Default -> {}
                    ConnectionState.Connected -> {
                        Log.d(TAG, "onConnected, this: $this, thread: ${Thread.currentThread().id}")
                        subscriber.subscribe()
                    }

                    ConnectionState.Connecting -> {
                        // nothing
                    }

                    ConnectionState.Disconnected -> {
                        Log.d(TAG, "onDisconnected")
                        data.update {
                            it.populateError(error = "Disconnected")
                        }
                    }

                    is ConnectionState.DisconnectedError -> {
                        Log.d(TAG, "onConnectionError: ${state.httpCode}, ${state.reason}")
                        data.update {
                            it.populateError(error = state.reason)
                        }
                    }

                    ConnectionState.Disconnecting -> {
                        // nothing
                    }
                }
            }

            subscriber.track.collect(this) { holder ->
                data.update { it.onTrack(holder.track, holder.mid ?: "") }
            }

            subscriber.rtcStatsReport.collect(this) { report ->
                //TODO: update the report structure
                data.update {
                    it.copy(
                        //statisticsData = MultiStreamStatisticsData.from(report)
                    )
                }
            }

            subscriber.signalingError.collect(this) {
                Log.d(TAG, "subscriber.signalingError: $it")
            }

            subscriber.vad.collect(this) {
                Log.d(TAG, "subscriber.vad: $it")
            }
        }
    }

    fun stop() {
        flowJob?.cancel()
        flowJob = null
    }

    fun connected() = subscriber.isSubscribed

    suspend fun disconnect() = subscriber.disconnect()

    suspend fun onActive(
        stream: String,
        tracksInfo: Array<String>,
        sourceId: String?
    ) {
        Log.d(TAG, "onActive: $stream, ${tracksInfo.toList()}, ${sourceId}")
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

    fun onInactive(streamId: String, sourceId: String?) {
        Log.d(TAG, "onInactive $streamId, ${sourceId}")
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

    fun onLayers(
        mid: String,
        activeLayers: Array<LayerData>,
        inactiveLayers: Array<String>
    ) {
        Log.d(
            TAG,
            "onLayers: $mid, ${activeLayers.contentToString()}, ${
                inactiveLayers.contentToString()
            }"
        )
        mid.let {
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
        val projected = data.value.trackProjectedData[video.id] ?: return

        if (projected.videoQuality == availablePreferredVideoQuality?.videoQuality) {
            return
        }

        Log.d(
            TAG, "project video ${video.id}, quality = $availablePreferredVideoQuality"
        )
        val projectionData = createProjectionData(video, availablePreferredVideoQuality)
        subscriber.project(video.sourceId ?: "", arrayListOf(projectionData))
        data.update {
            val mutableOldProjectedData = it.trackProjectedData.toMutableMap()
            mutableOldProjectedData[projectionData.mid] = projectedDataFrom(
                video.sourceId,
                availablePreferredVideoQuality?.videoQuality ?: VideoQuality.AUTO,
                projectionData.mid
            )
            it.copy(trackProjectedData = mutableOldProjectedData.toMap())
        }
    }

    private fun availablePreferredVideoQuality(
        video: MultiStreamingData.Video,
        preferredVideoQuality: VideoQuality
    ): LowLevelVideoQuality? {
        return data.value.trackLayerData[video.id]?.find {
            it.videoQuality == preferredVideoQuality
        } ?: if (preferredVideoQuality != VideoQuality.AUTO) availablePreferredVideoQuality(
            video,
            preferredVideoQuality.lower
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
            mid = audioTrack.id ?: "",
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