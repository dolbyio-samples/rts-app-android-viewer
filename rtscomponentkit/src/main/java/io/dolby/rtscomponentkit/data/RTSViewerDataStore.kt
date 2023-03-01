package io.dolby.rtscomponentkit.data

import android.app.Application
import android.content.Context
import com.millicast.AudioTrack
import com.millicast.Client
import com.millicast.LayerData
import com.millicast.VideoRenderer
import com.millicast.VideoTrack
import io.dolby.rtscomponentkit.domain.StreamingData
import io.dolby.rtscomponentkit.manager.SubscriptionListener
import io.dolby.rtscomponentkit.manager.SubscriptionManager
import io.dolby.rtscomponentkit.manager.SubscriptionManagerDelegate
import io.dolby.rtscomponentkit.manager.SubscriptionManagerInterface
import io.dolby.rtscomponentkit.utils.DispatcherProvider
import io.dolby.rtscomponentkit.utils.DispatcherProviderImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.webrtc.RTCStatsReport

class RTSViewerDataStore(
    context: Context,
    dispatcherProvider: DispatcherProvider = DispatcherProviderImpl
) {

    private val apiScope = CoroutineScope(dispatcherProvider.default + Job())

    private val videoRenderer: VideoRenderer = VideoRenderer(context)
    private val subscriptionDelegate = object : SubscriptionManagerDelegate {
        override fun onSubscribed() {
            _state.value = State.Subscribed
        }

        override fun onSubscribedError(reason: String) {
            _state.value = State.Error(SubscriptionError.SubscribeError(reason))
        }

        override fun onVideoTrack(track: VideoTrack, mid: String) {
            videoTrack = track
            track.setRenderer(videoRenderer)
        }

        override fun onAudioTrack(track: AudioTrack, mid: String) {
            audioTrack = track
        }

        override fun onStatsReport(report: RTCStatsReport) {
            TODO("Not yet implemented")
        }

        override fun onConnected() {
            _state.value = State.Connected
        }

        override fun onStreamActive() {
            _state.value = State.StreamActive
        }

        override fun onStreamInactive() {
            _state.value = State.StreamInactive
        }

        override fun onStreamStopped() {
            _state.value = State.StreamInactive
        }

        override fun onConnectionError(reason: String) {
            _state.value = State.Error(SubscriptionError.ConnectError(reason))
        }

        override fun onStreamLayers(
            mid: String?,
            activeLayers: Array<out LayerData>?,
            inactiveLayers: Array<out LayerData>?
        ) {
            TODO("Not yet implemented")
        }
    }
    private val subscriptionListener = SubscriptionListener(subscriptionDelegate)
    private val subscriptionManager: SubscriptionManagerInterface =
        SubscriptionManager(subscriptionDelegate, subscriptionListener)

    private var _state: MutableStateFlow<State> = MutableStateFlow(State.Disconnected)
    val state: Flow<State> = _state

    private var streamDetail: StreamingData? = null
    private var audioTrack: AudioTrack? = null
    private var videoTrack: VideoTrack? = null

    init {
        Client.initMillicastSdk(context)
    }

    fun connect() {
        apiScope.launch {
            streamDetail?.let {
                subscriptionManager.connect(it.streamName, it.accountId)
            }
        }
    }

    fun startSubscribe() {
        apiScope.launch {
            subscriptionManager.startSubscribe()
        }
    }

    fun stopSubscribe() {
        apiScope.launch {
            subscriptionManager.stopSubscribe()
        }
    }

    sealed class SubscriptionError {
        class SubscribeError(reason: String) : SubscriptionError()
        class ConnectError(reason: String) : SubscriptionError()
    }

    sealed class State {
        object Connecting : State()
        object Connected : State()
        object Subscribing : State()
        object Subscribed : State()
        object StreamActive : State()
        object StreamInactive : State()
        object Disconnected : State()
        class Error(error: SubscriptionError) : State()
    }
}