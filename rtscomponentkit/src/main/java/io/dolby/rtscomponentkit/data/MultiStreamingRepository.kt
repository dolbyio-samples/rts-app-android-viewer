package io.dolby.rtscomponentkit.data

import android.content.Context
import android.util.Log
import com.millicast.AudioTrack
import com.millicast.LayerData
import com.millicast.Subscriber
import com.millicast.Subscriber.ProjectionData
import com.millicast.VideoTrack
import io.dolby.rtscomponentkit.data.MultiStreamingData.Companion.audio
import io.dolby.rtscomponentkit.data.MultiStreamingData.Companion.video
import io.dolby.rtscomponentkit.domain.StreamingData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import org.webrtc.RTCStatsReport
import java.util.Optional
import kotlin.jvm.optionals.getOrNull

data class MultiStreamingData(
    val videoTracks: List<Video> = emptyList(),
    val audioTracks: List<Audio> = emptyList(),
    val selectedVideoTrackId: String? = null,
    val selectedAudioTrackId: String? = null,
    val viewerCount: Int = 0,
    val pendingVideoTracks: List<PendingTrack> = emptyList(),
    val pendingAudioTracks: List<PendingTrack> = emptyList(),
    val error: String? = null,
    val isSubscribed: Boolean = false,
    val streamingData: StreamingData? = null
) {
    class Video(val id: String?, val videoTrack: VideoTrack, val sourceId: String?)
    class Audio(val id: String?, val audioTrack: AudioTrack)

    class PendingTrack(val mediaType: String, val trackId: String, val sourceId: String)

    internal class PendingTracks(
        val videoTracks: List<PendingTrack>,
        val audioTracks: List<PendingTrack>
    )

    fun appendMainVideoTrackAndClone(videoTrack: VideoTrack, mid: String?): MultiStreamingData {
        val videoTracks = videoTracks.toMutableList().apply {
            add(Video(mid, videoTrack, null))
        }
        return copy(videoTracks = videoTracks)
    }

    fun appendAudioTrackAndClone(audioTrack: AudioTrack, id: String?): MultiStreamingData {
        val audioTracks = audioTracks.toMutableList().apply {
            add(Audio(id, audioTrack))
        }
        return copy(audioTracks = audioTracks)
    }

    fun getPendingVideoTrackInfoOrNull(): PendingTrack? = pendingVideoTracks.firstOrNull()

    fun appendOtherVideoTrackAndClone(
        pendingTrack: PendingTrack,
        videoTrack: VideoTrack,
        mid: String?,
        sourceId: String
    ): MultiStreamingData {
        val pendingVideoTracks = pendingVideoTracks.toMutableList().apply { remove(pendingTrack) }

        val videoTracks = videoTracks.toMutableList().apply {
            add(Video(mid, videoTrack, sourceId))
        }
        return copy(videoTracks = videoTracks, pendingVideoTracks = pendingVideoTracks)
    }

    internal fun addPendingTracksAndClone(pendingTracks: PendingTracks): MultiStreamingData {
        val pendingVideoTracks = pendingVideoTracks.toMutableList().apply {
            addAll(pendingTracks.videoTracks)
        }
        val pendingAudioTracks = pendingAudioTracks.toMutableList().apply {
            addAll(pendingTracks.audioTracks)
        }
        return copy(
            pendingVideoTracks = pendingVideoTracks,
            pendingAudioTracks = pendingAudioTracks
        )
    }

    companion object {
        const val video = "video"
        const val audio = "audio"

        internal fun parseTracksInfo(
            tracksInfo: Array<out String>,
            sourceId: String
        ): PendingTracks {
            val videoTracks = mutableListOf<PendingTrack>()
            val audioTracks = mutableListOf<PendingTrack>()
            tracksInfo.forEach { trackInfo ->
                val trackInfoSplit = trackInfo.split("/")
                trackInfoSplit.firstOrNull()?.let { mediaType ->
                    when (mediaType) {
                        video -> videoTracks.add(
                            PendingTrack(
                                mediaType,
                                trackInfoSplit[1],
                                sourceId
                            )
                        )
                        audio -> audioTracks.add(
                            PendingTrack(
                                mediaType,
                                trackInfoSplit[1],
                                sourceId
                            )
                        )
                        else -> Unit
                    }
                }
            }
            return PendingTracks(videoTracks = videoTracks, audioTracks = audioTracks)
        }
    }
}

class MultiStreamingRepository(context: Context, millicastSdk: MillicastSdk) {
    private val _data = MutableStateFlow(MultiStreamingData())
    val data: StateFlow<MultiStreamingData> = _data.asStateFlow()
    private var listener: Listener? = null

    init {
        millicastSdk.init(context)
    }

    fun connect(streamingData: StreamingData) {
        if (listener?.connected() == true) {
            return
        }
        val listener = Listener(streamingData, _data)
        this.listener = listener
        val subscriber = Subscriber.createSubscriber(listener)
        listener.subscriber = subscriber

        subscriber.credentials = credential(subscriber.credentials, streamingData)

        Log.d(TAG, "Connecting ...")

        subscriber.connect()
        _data.update { data -> data.copy(streamingData = streamingData) }
    }

    fun disconnect() {
        _data.update { MultiStreamingData() }
        listener?.disconnect()
    }

    private fun credential(
        credentials: Subscriber.Credential,
        streamingData: StreamingData
    ): Subscriber.Credential {
        credentials.streamName = streamingData.streamName
        credentials.accountId = streamingData.accountId
        if (streamingData.useDevEnv) {
            credentials.apiUrl = "https://director-dev.millicast.com/api/director/subscribe"
        } else {
            credentials.apiUrl = "https://director.millicast.com/api/director/subscribe"
        }
        return credentials
    }

    fun updateSelectedVideoTrackId(sourceId: String?) {
        _data.update { data ->
            val oldSelectedVideoTrackId = data.selectedVideoTrackId
            data.videoTracks.find { it.sourceId == oldSelectedVideoTrackId }?.videoTrack?.removeRenderer()
            data.copy(selectedVideoTrackId = sourceId)
        }
    }

    private class Listener(
        private val streamingData: StreamingData,
        private val data: MutableStateFlow<MultiStreamingData>
    ) : Subscriber.Listener {

        var subscriber: Subscriber? = null

        fun connected(): Boolean = subscriber?.isSubscribed ?: false

        fun disconnect() {
            subscriber?.disconnect()
            subscriber = null
        }

        override fun onConnected() {
            val subscriber = subscriber ?: return

            val options = Subscriber.Option().apply {
                autoReconnect = true
                disableAudio = streamingData.disableAudio
                forcePlayoutDelay = streamingData.useDevEnv
                videoJitterMinimumDelayMs = Optional.of(streamingData.videoJitterMinimumDelayMs)
            }

            subscriber.setOptions(options)
            subscriber.subscribe()
        }

        override fun onDisconnected() {
            Log.d(TAG, "onDisconnected")
        }

        override fun onConnectionError(p0: Int, p1: String?) {
            Log.d(TAG, "onConnectionError: $p0, $p1")
            data.update {
                it.copy(error = p1 ?: "Unknown error")
            }
        }

        override fun onSignalingError(p0: String?) {
            Log.d(TAG, "onSignalingError: $p0")
            data.update {
                it.copy(error = p0 ?: "Signaling error")
            }
        }

        override fun onStatsReport(p0: RTCStatsReport?) {
            TODO("Not yet implemented")
        }

        override fun onViewerCount(p0: Int) {
            Log.d(TAG, "onViewerCount: $p0")
            data.update { data -> data.copy(viewerCount = p0) }
        }

        override fun onSubscribed() {
            Log.d(TAG, "onSubscribed")
            data.update { data -> data.copy(isSubscribed = true) }
        }

        override fun onSubscribedError(p0: String?) {
            Log.d(TAG, "onSubscribedError: $p0")
            data.update {
                it.copy(error = p0 ?: "Subscribed error")
            }
        }

        override fun onTrack(p0: VideoTrack, p1: Optional<String>) {
            val mid = p1.getOrNull()
            Log.d(TAG, "onVideoTrack: $mid, $p0")
            data.update { data ->
                if (data.videoTracks.isEmpty()) {
                    data.appendMainVideoTrackAndClone(p0, mid)
                } else {
                    data.getPendingVideoTrackInfoOrNull()?.let { trackInfo ->
                        val projectionData = createProjectionData(mid, trackInfo)
                        subscriber?.project(trackInfo.sourceId, arrayListOf(projectionData))
                        data.appendOtherVideoTrackAndClone(trackInfo, p0, mid, trackInfo.sourceId)
                    } ?: data
                }
            }
        }

        override fun onTrack(p0: AudioTrack, p1: Optional<String>) {
            Log.d(TAG, "onAudioTrack: ${p1.getOrNull()}, $p0")
            data.update { data -> data.appendAudioTrackAndClone(p0, p1.getOrNull()) }
        }

        override fun onFrameMetadata(p0: Int, p1: Int, p2: ByteArray?) {
            TODO("Not yet implemented")
        }

        override fun onActive(
            stream: String,
            tracksInfo: Array<out String>,
            optionalSourceId: Optional<String>
        ) {
            Log.d(TAG, "onActive: $stream, $tracksInfo, ${optionalSourceId.getOrNull()}")
            val sourceId = optionalSourceId.getOrNull() ?: return
            val pendingTracks = MultiStreamingData.parseTracksInfo(tracksInfo, sourceId)
            val data = data.updateAndGet { data ->
                data.addPendingTracksAndClone(pendingTracks)
            }
            if (data.isSubscribed) {
                repeat(pendingTracks.videoTracks.size) {
                    subscriber?.addRemoteTrack(video)
                }
                repeat(pendingTracks.audioTracks.size) {
                    subscriber?.addRemoteTrack(audio)
                }
            }
        }

        override fun onInactive(p0: String, p1: Optional<String>) {
            data.update { data ->
                data.videoTracks.filter { it.sourceId == p1.getOrNull() }
                    .forEach { it.videoTrack.removeRenderer() }
                val temp = data.videoTracks.toMutableList()
                temp.removeAll { it.sourceId == p1.getOrNull() }
                return@update if (data.selectedVideoTrackId == p1.getOrNull()) {
                    data.copy(selectedVideoTrackId = null, videoTracks = temp)
                } else {
                    data.copy(videoTracks = temp)
                }
            }
        }

        override fun onStopped() {
            Log.d(TAG, "onStopped")
        }

        override fun onVad(p0: String?, p1: Optional<String>?) {
            TODO("Not yet implemented")
        }

        override fun onLayers(p0: String?, p1: Array<out LayerData>?, p2: Array<out LayerData>?) {
            TODO("Not yet implemented")
        }
    }

    companion object {
        private const val TAG = "MultiStreamingRepository"

        fun createProjectionData(mid: String?, pendingTrack: MultiStreamingData.PendingTrack) =
            ProjectionData().also {
                it.mid = mid
                it.trackId = pendingTrack.trackId
                it.media = pendingTrack.mediaType
                it.layer = null
            }
    }
}
