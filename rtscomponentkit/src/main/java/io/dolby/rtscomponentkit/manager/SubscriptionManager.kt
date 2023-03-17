package io.dolby.rtscomponentkit.manager

import android.util.Log
import com.millicast.LayerData
import com.millicast.Subscriber

private const val tag = "SubscriptionManager"
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

    override suspend fun connect(streamName: String, accountID: String): Boolean {
        subscriber = Subscriber.createSubscriber(subscriptionListener)
        if (subscriber == null) {
            Log.d(tag, "Failed! Subscriber is not available.")
            return false
        }

        // Create Subscriber if not present
        subscriber?.let {
            if (it.isConnected) {
                Log.d(tag, "Not doing as we're already connected!")
                return true
            }

            Log.d(tag, "Set Credentials.")
            val credentials = it.credentials
            credentials.streamName = streamName
            credentials.accountId = accountID
            credentials.apiUrl = "https://director.millicast.com/api/director/subscribe"
            it.credentials = credentials
        }
        // Connect Subscriber.
        Log.d(tag, "Trying...")
        var success = false
        try {
            success = subscriber?.connect() ?: false
        } catch (e: Exception) {
            Log.d(tag, "${e.message}")
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
            Log.d(tag, "${e.message}")
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
            Log.d(tag, "${e.message}")
        }

        // Disconnect from Millicast.
        try {
            subscriber?.disconnect()
        } catch (e: java.lang.Exception) {
            success = false
            Log.d(tag, "${e.message}")
        }
        enableStatsSub(0)
        return success
    }

    override suspend fun selectLayer(layer: LayerData?): Boolean {
        TODO("Not yet implemented")
    }

    private fun enableStatsSub(enable: Int) {
        subscriber?.let {
            try {
                if (enable > 0) {
                    it.getStats(enable)
                    Log.d(tag, "YES. Interval: $enable ms.")
                } else {
                    it.getStats(0)
                    Log.d(tag, "NO.")
                }
            } catch (e: IllegalStateException) {
                Log.e(tag, e.message ?: e.toString())
            }
        }
    }
}
