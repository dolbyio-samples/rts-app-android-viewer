package io.dolby.rtscomponentkit.manager

import android.util.Log
import com.millicast.AudioTrack
import com.millicast.LayerData
import com.millicast.Subscriber
import com.millicast.VideoTrack
import io.dolby.rtscomponentkit.legacy.MillicastManager
import io.dolby.rtscomponentkit.legacy.Utils.logD
import org.webrtc.RTCStatsReport


interface SubscriptionManagerDelegate {
    fun onSubscribed()
    fun onSubscribedError(reason: String)
    fun onVideoTrack(track: VideoTrack, mid: String)
    fun onAudioTrack(track: AudioTrack, mid: String)
    fun onStatsReport(report: RTCStatsReport)
    fun onConnected()
    fun onStreamActive()
    fun onStreamInactive()
    fun onStreamStopped()
    fun onConnectionError(reason: String)
    fun onStreamLayers(
        mid: String?,
        activeLayers: Array<out LayerData>?,
        inactiveLayers: Array<out LayerData>?
    )
}

interface SubscriptionManagerInterface {
    fun connect(streamName: String, accountID: String): Boolean
    fun startSubscribe(): Boolean
    fun stopSubscribe(): Boolean
    fun selectLayer(layer: LayerData?): Boolean
}

class SubscriptionManager(val delegate: SubscriptionManagerDelegate, val subscriptionListener: SubscriptionListener) : SubscriptionManagerInterface {
    var subscriber: Subscriber? = null

    private val TAG = SubscriptionManager::class.simpleName

    override fun connect(streamName: String, accountID: String): Boolean {
        val logTag = "[Sub][Con] "
        subscriber = Subscriber.createSubscriber(subscriptionListener)
        if (subscriber == null) {
            logD(MillicastManager.TAG, logTag + "Failed! Subscriber is not available.")
            return false
        }

        // Create Subscriber if not present
        subscriber?.let {
            if (it.isConnected) {
                logD(MillicastManager.TAG, logTag + "Not doing as we're already connected!")
                return true
            }

            logD(MillicastManager.TAG, logTag + "Set Credentials.")
            val credentials = it.credentials
            credentials.streamName = streamName
            credentials.accountId = accountID
            it.credentials = credentials
        }
        // Connect Subscriber.
        logD(MillicastManager.TAG, logTag + "Trying...")
        var success = false
        try {
            success = subscriber?.connect() ?: false
        } catch (e: Exception) {
            Log.d(TAG, "${e.message}");
        }

        return success
    }

    override fun startSubscribe(): Boolean {
        // Subscribe to Millicast
        var success = true
        try {
            subscriber?.subscribe()
        } catch (e: Exception) {
            success = false
            logD(MillicastManager.TAG, "${e.message}")
        }
        enableStatsSub(10000)
        return success
    }

    override fun stopSubscribe(): Boolean {
        // Stop subscribing to Millicast.
        var success = true
        try {
            subscriber?.unsubscribe()
        } catch (e: Exception) {
            success = false
            logD(MillicastManager.TAG, "${e.message}")
        }

        // Disconnect from Millicast.
        try {
            subscriber?.disconnect()
        } catch (e: java.lang.Exception) {
            success = false
            logD(MillicastManager.TAG, "${e.message}")
        }
        enableStatsSub(0)
        return success
    }

    override fun selectLayer(layer: LayerData?): Boolean {
        TODO("Not yet implemented")
    }

    fun enableStatsSub(enable: Int) {
        subscriber?.let {
            val logTag = "[Sub][Stats][Enable] "
            if (enable > 0) {
                it.getStats(enable)
                logD(MillicastManager.TAG, logTag + "YES. Interval: " + enable + "ms.")
            } else {
                it.getStats(0)
                logD(MillicastManager.TAG, logTag + "NO.")
            }
        }
    }
}

