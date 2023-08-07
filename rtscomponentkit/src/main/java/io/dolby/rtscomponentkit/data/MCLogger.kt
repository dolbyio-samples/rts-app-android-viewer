package io.dolby.rtscomponentkit.data

import android.util.Log
import com.millicast.LogLevel
import com.millicast.Logger
import com.millicast.LoggerInterface
import java.io.File
import java.io.FileOutputStream
import java.lang.Exception

class MCLogger(path: String, timeStamp: String) : LoggerInterface {
    private val TAG = "MCLogger"
    private val fileName = "${path}/${timeStamp}_sdklog.txt"

    init {
        Logger.setLoggerListener(this)
        Logger.disableWebsocketLogs(true)
    }

    override fun onLog(message: String?, logLevel: LogLevel?) {
        Log.d(TAG, message ?: "")
        try {
            val file = File(fileName)
            val outputStream = FileOutputStream(
                file,
                true
            ).apply {
                write(message?.toByteArray())
                flush()
            }
            outputStream.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error writing log file")
        }
    }
}