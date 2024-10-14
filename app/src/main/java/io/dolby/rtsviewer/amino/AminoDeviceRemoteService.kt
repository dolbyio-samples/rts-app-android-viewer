package io.dolby.rtsviewer.amino

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import com.aminocom.device.IDeviceRemoteService
import com.squareup.moshi.Moshi
import io.dolby.rtscomponentkit.domain.StreamingConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelChildren

class AminoDeviceRemoteService(private val aminoDevice: AminoDevice, private val moshi: Moshi) {

    private var isConnecting = false
    private var remoteService: IDeviceRemoteService? = null
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    private val serviceConnectionListener = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            isConnecting = false
            remoteService = IDeviceRemoteService.Stub.asInterface(service)
            buildDeviceConfig()
        }

        override fun onServiceDisconnected(className: ComponentName) {
            isConnecting = false
            remoteService = null
        }
    }

    fun connect(bindService: (intent: Intent, serviceConnection: ServiceConnection, flag: Int) -> Unit) {
        if (isConnecting) {
            // already trying to connect
            return
        }
        if (remoteService != null) {
            // already connected
            return
        }
        isConnecting = true
        val intent = Intent()
            .setComponent(
                ComponentName(DEVICE_REMOTE_SERVICE_PACKAGE, DEVICE_REMOTE_SERVICE_CLASS)
            )
        bindService(intent, serviceConnectionListener, Context.BIND_AUTO_CREATE)
    }

    fun disconnect(unbindService: (serviceConnection: ServiceConnection) -> Unit) {
        if (remoteService != null) {
            unbindService(serviceConnectionListener)
            isConnecting = false
            remoteService = null
        }
        coroutineScope.coroutineContext.cancelChildren()
    }

    private fun buildDeviceConfig() {
        try {
            val jsonArgs = remoteService?.getDeviceParameter("tvapp.mw_args", "")
            jsonArgs?.takeIf { it.isNotEmpty() }?.let {
                moshi.adapter(StreamingConfig::class.java).lenient().fromJson(it)
                    ?.let { config ->
                        aminoDevice.updateConfig(config)
                        Log.d("tvapp.mw_args", aminoDevice.config.value.toString())
                    }
            }
        } catch (e: Exception) {
            aminoDevice.updateConfig(null)
        }
    }

    companion object {
        private const val DEVICE_REMOTE_SERVICE_PACKAGE = "com.aminocom.device"
        private const val DEVICE_REMOTE_SERVICE_CLASS = "com.aminocom.device.DeviceRemoteService"
    }
}
