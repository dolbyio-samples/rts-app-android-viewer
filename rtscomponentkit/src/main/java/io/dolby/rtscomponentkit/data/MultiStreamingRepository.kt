package io.dolby.rtscomponentkit.data

import android.content.Context
import android.util.Log
import com.millicast.AudioTrack
import com.millicast.LayerData
import com.millicast.Subscriber
import com.millicast.VideoTrack
import io.dolby.rtscomponentkit.domain.StreamingData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.webrtc.RTCStatsReport
import java.util.Optional
import kotlin.jvm.optionals.getOrNull

data class MultiStreamingData(
    val videoTracks: List<Video> = emptyList(),
    val audioTracks: List<Audio> = emptyList(),
    val viewerCount: Int = 0,
    val error: String? = null
) {
    data class Video(val id: String?, val videoTrack: VideoTrack)
    data class Audio(val id: String?, val audioTrack: AudioTrack)

    fun appendAndCopy(videoTrack: VideoTrack, id: Optional<String>): MultiStreamingData {
        val videoTracks = videoTracks.toMutableList().apply {
            add(Video(id.getOrNull(), videoTrack))
        }
        return copy(videoTracks = videoTracks)
    }

    fun appendAndCopy(audioTrack: AudioTrack, id: Optional<String>): MultiStreamingData {
        val audioTracks = audioTracks.toMutableList().apply {
            add(Audio(id.getOrNull(), audioTrack))
        }
        return copy(audioTracks = audioTracks)
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
    }

    fun disconnect() {
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
            disconnect()
        }

        override fun onSignalingError(p0: String?) {
            TODO("Not yet implemented")
        }

        override fun onStatsReport(p0: RTCStatsReport?) {
            TODO("Not yet implemented")
        }

        override fun onViewerCount(p0: Int) {
            Log.d(TAG, "onViewerCount: $p0")
            data.update { data -> data.copy(viewerCount = p0) }
        }

        override fun onSubscribed() {
            TODO("Not yet implemented")
        }

        override fun onSubscribedError(p0: String?) {
            TODO("Not yet implemented")
        }

        override fun onTrack(p0: VideoTrack, p1: Optional<String>) {
            Log.d(TAG, "onVideoTrack: ${p1.getOrNull()}, $p0")
            data.update { data -> data.appendAndCopy(p0, p1) }
        }

        override fun onTrack(p0: AudioTrack, p1: Optional<String>) {
            Log.d(TAG, "onAudioTrack: ${p1.getOrNull()}, $p0")
            data.update { data -> data.appendAndCopy(p0, p1) }
        }

        override fun onFrameMetadata(p0: Int, p1: Int, p2: ByteArray?) {
            TODO("Not yet implemented")
        }

        override fun onActive(p0: String, p1: Array<out String>, p2: Optional<String>) {
            Log.d(TAG, "onActive: $p0, $p1, ${p2.getOrNull()}")
        }

        override fun onInactive(p0: String?, p1: Optional<String>?) {
            TODO("Not yet implemented")
        }

        override fun onStopped() {
            TODO("Not yet implemented")
        }

        override fun onVad(p0: String?, p1: Optional<String>?) {
            TODO("Not yet implemented")
        }

        override fun onLayers(p0: String?, p1: Array<out LayerData>?, p2: Array<out LayerData>?) {
            TODO("Not yet implemented")
        }
    }

    companion object {
        private const val TAG = "=====> MultiStreamingDataStore"
    }
}
