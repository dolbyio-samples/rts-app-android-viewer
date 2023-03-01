package io.dolby.rtscomponentkit.manager

import android.util.Log
import com.millicast.AudioTrack
import com.millicast.LayerData
import com.millicast.Subscriber
import com.millicast.VideoTrack
import org.webrtc.RTCStatsReport
import java.util.Optional

class SubscriptionListener(val delegate: SubscriptionManagerDelegate) : Subscriber.Listener {
    override fun onConnected() {
        delegate.onConnected()
    }

    override fun onConnectionError(p0: String?) {
        delegate.onConnectionError("$p0")
    }

    override fun onSignalingError(p0: String?) {
        Log.d(SubscriptionListener::class.simpleName, "onSignalingError $p0")
    }

    override fun onStatsReport(p0: RTCStatsReport?) {
        p0?.let {
            delegate.onStatsReport(p0)
        }
    }

    override fun onViewerCount(p0: Int) {
        TODO("Not yet implemented")
    }

    override fun onSubscribed() {
        delegate.onSubscribed()
    }

    override fun onSubscribedError(p0: String?) {
        delegate.onSubscribedError("$p0")
    }

    override fun onTrack(p0: VideoTrack?, p1: Optional<String>?) {
        p0?.let {
            delegate.onVideoTrack(p0, "$p1")
        }
    }

    override fun onTrack(p0: AudioTrack?, p1: Optional<String>?) {
        p0?.let {
            delegate.onAudioTrack(p0, "$p1")
        }
    }

    override fun onActive(p0: String?, p1: Array<out String>?, p2: Optional<String>?) {
        delegate.onStreamActive()
    }

    override fun onInactive(p0: String?, p1: Optional<String>?) {
        delegate.onStreamInactive()
    }

    override fun onStopped() {
        delegate.onStreamStopped()
    }

    override fun onVad(p0: String?, p1: Optional<String>?) {
        TODO("Not yet implemented")
    }

    override fun onLayers(p0: String?, p1: Array<out LayerData>?, p2: Array<out LayerData>?) {
        delegate.onStreamLayers(p0, p1, p2)
    }
}