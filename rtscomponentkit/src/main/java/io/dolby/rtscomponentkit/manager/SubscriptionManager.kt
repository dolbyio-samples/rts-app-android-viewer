package io.dolby.rtscomponentkit.manager

import android.util.Log
import com.millicast.LayerData
import com.millicast.Subscriber
import io.dolby.rtscomponentkit.legacy.MillicastManager
import io.dolby.rtscomponentkit.legacy.Utils.logD

interface SubscriptionManagerInterface {
    suspend fun connect(streamName: String, accountID: String): Boolean
    suspend fun startSubscribe(): Boolean
    suspend fun stopSubscribe(): Boolean
    suspend fun selectLayer(layer: LayerData?): Boolean
}

class SubscriptionManager(
    private val subscriptionListener: Subscriber.Listener
) : SubscriptionManagerInterface {
    private var subscriber: Subscriber? = null

    private val TAG = SubscriptionManager::class.simpleName

    override suspend fun connect(streamName: String, accountID: String): Boolean {
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
            credentials.apiUrl = "https://director.millicast.com/api/director/subscribe"
            it.credentials = credentials
        }
        // Connect Subscriber.
        logD(MillicastManager.TAG, logTag + "Trying...")
        var success = false
        try {
            success = subscriber?.connect() ?: false
        } catch (e: Exception) {
            Log.d(MillicastManager.TAG, "${e.message}");
        }

        return success
    }

    override suspend fun startSubscribe(): Boolean {
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

    override suspend fun stopSubscribe(): Boolean {
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

    override suspend fun selectLayer(layer: LayerData?): Boolean {
        TODO("Not yet implemented")
    }

    private fun enableStatsSub(enable: Int) {
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

