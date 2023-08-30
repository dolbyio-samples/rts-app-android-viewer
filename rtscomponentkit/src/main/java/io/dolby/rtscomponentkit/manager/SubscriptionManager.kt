package io.dolby.rtscomponentkit.manager

import android.os.Environment
import android.util.Log
import com.millicast.LayerData
import com.millicast.Subscriber
import io.dolby.rtscomponentkit.data.MCLogger
import io.dolby.rtscomponentkit.domain.StreamingData
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Optional

internal const val TAG = "RTSSubscriptionManager"

interface SubscriptionManagerInterface {
    suspend fun connect(streamingData: StreamingData): Boolean
    suspend fun startSubscribe(): Boolean
    suspend fun stopSubscribe(): Boolean
    suspend fun selectLayer(layer: LayerData?): Boolean
}

class SubscriptionManager(
    private val subscriptionListener: Subscriber.Listener
) : SubscriptionManagerInterface {
    private var subscriber: Subscriber? = null
    private var streamingData: StreamingData? = null
    private var logger: MCLogger? = null

    override suspend fun connect(streamingData: StreamingData): Boolean {
        logger = null
        subscriber = Subscriber.createSubscriber(subscriptionListener)
        if (subscriber == null) {
            Log.d(TAG, "Failed! Subscriber is not available.")
            return false
        }

        this.streamingData = streamingData

        // Create Subscriber if not present
        subscriber?.let {
            if (it.isConnected) {
                Log.d(TAG, "Not doing as we're already connected!")
                return true
            }

            Log.d(TAG, "Set Credentials.")
            val credentials = it.credentials
            credentials.streamName = streamingData.streamName
            credentials.accountId = streamingData.accountId
            if (streamingData.useDevEnv) {
                credentials.apiUrl = "https://director-dev.millicast.com/api/director/subscribe"
            } else {
                credentials.apiUrl = "https://director.millicast.com/api/director/subscribe"
            }
            it.credentials = credentials
        }
        // Connect Subscriber.
        Log.d(TAG, "Trying...")
        var success = false
        try {
            success = subscriber?.connect() ?: false
        } catch (e: Exception) {
            Log.d(TAG, "${e.message}")
        }

        return success
    }

    private fun createDirectoryIfNotExists(directoryPath: String) {
        val directory = File(directoryPath)
        if (!directory.exists()) {
            val isDirectoryCreated = directory.mkdirs()
            if (isDirectoryCreated) {
                Log.d(TAG, "Directory was successfully created")
            } else {
                Log.d(TAG, "Directory creation failed")
            }
        } else {
            Log.d(TAG, "Directory already exists")
        }
    }

    override suspend fun startSubscribe(): Boolean {
        // Subscribe to Millicast
        var success = true
        try {
            streamingData?.let { sd ->
                val path = Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOWNLOADS
                ).absolutePath + "/Ultra"
                createDirectoryIfNotExists(path)
                val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME.withZone(
                    ZoneId.from(
                        ZoneOffset.UTC
                    )
                )
                val timeStamp = formatter.format(Instant.now().truncatedTo(ChronoUnit.SECONDS))
                    .replace(':', '-')
                // Set Subscriber Options
                val currentOptionSub = Subscriber.Option().apply {
                    autoReconnect = true
                    disableAudio = sd.disableAudio
                    forcePlayoutDelay = sd.useDevEnv
                    videoJitterMinimumDelayMs = Optional.of(sd.videoJitterMinimumDelayMs)
                    if (sd.rtcLogs) {
                        rtcEventLogOutputPath = Optional.of(path + "/${timeStamp}_rtclogs.proto")
                        logger = MCLogger(path = path, timeStamp = timeStamp.toString())
                    }
                }

                subscriber?.setOptions(currentOptionSub)
            }

            subscriber?.subscribe()
        } catch (e: Exception) {
            success = false
            Log.d(TAG, "${e.message}")
        }
        enableStatsSub(1000)
        return success
    }

    override suspend fun stopSubscribe(): Boolean {
        // Stop subscribing to Millicast.
        var success = true
        try {
            success = subscriber?.unsubscribe() ?: false
        } catch (e: Exception) {
            success = false
            Log.d(TAG, "${e.message}")
        }

        // Disconnect from Millicast.
        try {
            success = subscriber?.disconnect() ?: false
        } catch (e: java.lang.Exception) {
            success = false
            Log.d(TAG, "${e.message}")
        }
        enableStatsSub(0)
        subscriber = null
        return success
    }

    override suspend fun selectLayer(layer: LayerData?): Boolean {
        val layerData: Optional<LayerData>? = layer?.let { Optional.of(it) } ?: Optional.empty()
        return subscriber?.select(layerData) ?: false
    }

    private fun enableStatsSub(enable: Int) {
        subscriber?.let {
            try {
                if (enable > 0) {
                    it.getStats(enable)
                    Log.d(TAG, "YES. Interval: $enable ms.")
                } else {
                    it.getStats(0)
                    Log.d(TAG, "NO.")
                }
            } catch (e: IllegalStateException) {
                Log.e(TAG, e.message ?: e.toString())
            }
        }
    }
}
