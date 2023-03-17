package io.dolby.rtscomponentkit.data

import android.content.Context
import android.util.Log
import com.millicast.AudioPlayback
import com.millicast.AudioTrack
import com.millicast.Client
import com.millicast.LayerData
import com.millicast.Media
import com.millicast.Subscriber
import com.millicast.VideoTrack
import io.dolby.rtscomponentkit.manager.SubscriptionManager
import io.dolby.rtscomponentkit.manager.SubscriptionManagerInterface
import io.dolby.rtscomponentkit.utils.DispatcherProvider
import io.dolby.rtscomponentkit.utils.DispatcherProviderImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.webrtc.RTCStatsReport
import java.util.Optional

private const val tag = "RTSDataStore"
class RTSViewerDataStore(
    context: Context,
    dispatcherProvider: DispatcherProvider = DispatcherProviderImpl
) {
    private val apiScope = CoroutineScope(dispatcherProvider.default + Job())

    private val subscriptionDelegate = object : Subscriber.Listener {
        override fun onSubscribed() {
            _state.value = State.Subscribed
        }

        override fun onSubscribedError(reason: String) {
            Log.d(tag, "onSubscribedError: $reason")
            _state.value = State.Error(SubscriptionError.SubscribeError(reason))
        }

        override fun onTrack(track: VideoTrack, p1: Optional<String>?) {
            Log.d(tag, "onVideoTrack")
            _state.value = State.VideoTrackReady(track)
        }

        override fun onTrack(track: AudioTrack, p1: Optional<String>?) {
            Log.d(tag, "onAudioTrack")
            _state.value = State.AudioTrackReady(track)
        }

        override fun onStatsReport(report: RTCStatsReport) {
            Log.d(tag, "onStatsReport")
        }

        override fun onViewerCount(p0: Int) {
            Log.d("Subscriber", "onViewerCount")
        }

        override fun onConnected() {
            Log.d(tag, "onConnected")
            startSubscribe()
        }

        override fun onActive(p0: String?, p1: Array<out String>?, p2: Optional<String>?) {
            Log.d(tag, "onActive")
            _state.value = State.StreamActive
        }

        override fun onInactive(p0: String?, p1: Optional<String>?) {
            Log.d(tag, "onInactive")
            _state.value = State.StreamInactive
        }

        override fun onStopped() {
            Log.d(tag, "onStopped")
            _state.value = State.StreamInactive
        }

        override fun onVad(p0: String?, p1: Optional<String>?) {
            Log.d(tag, "onVad")
            TODO("Not yet implemented")
        }

        override fun onConnectionError(reason: String) {
            Log.d(tag, "onConnectionError: $reason")
            _state.value = State.Error(SubscriptionError.ConnectError(reason))
        }

        override fun onSignalingError(reason: String?) {
            Log.d(tag, "onSignalingError: $reason")
        }

        override fun onLayers(p0: String?, p1: Array<out LayerData>?, p2: Array<out LayerData>?) {
            Log.d(tag, "onLayers: $p0")
        }
    }

    private val subscriptionManager: SubscriptionManagerInterface =
        SubscriptionManager(subscriptionDelegate)

    private var _state: MutableStateFlow<State> = MutableStateFlow(State.Disconnected)
    val state: Flow<State> = _state

    private var media: Media
    private var audioPlayback: ArrayList<AudioPlayback>? = null

    init {
        Client.initMillicastSdk(context)
        media = Media.getInstance(context)
        audioPlayback = media.audioPlayback
    }

    fun connect(streamName: String, accountId: String) = apiScope.launch {
        subscriptionManager.connect(streamName, accountId)
    }

    private fun startSubscribe() = apiScope.launch {
        subscriptionManager.startSubscribe()
    }

    fun stopSubscribe() = apiScope.launch {
        subscriptionManager.stopSubscribe()
    }

    /**
     * Start the playback of selected audioPlayback if available.
     */
    fun audioPlaybackStart() {
        if (audioPlayback == null) {
            Log.d(tag, "Creating new audioPlayback...")

            audioPlayback = media.audioPlayback
        } else {
            Log.d(tag, "Using existing audioPlayback...")
        }
        Log.d(tag, "AudioPlayback is: $audioPlayback")

        audioPlayback?.let {
            it[0].initPlayback()
            Log.d(tag, "OK. Playback initiated.")
        }
    }

    sealed class SubscriptionError(val reason: String) {
        class SubscribeError(reason: String) : SubscriptionError(reason = reason)
        class ConnectError(reason: String) : SubscriptionError(reason = reason)
    }

    sealed class State {
        object Subscribed : State()
        object StreamActive : State()
        object StreamInactive : State()
        object Disconnected : State()
        class Error(val error: SubscriptionError) : State()
        class AudioTrackReady(val audioTrack: AudioTrack) : State()
        class VideoTrackReady(val videoTrack: VideoTrack) : State()
    }
}
