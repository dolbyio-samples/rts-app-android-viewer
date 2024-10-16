package io.dolby.rtsviewer.amino

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import com.aminocom.device.IDeviceRemoteService
import com.squareup.moshi.Moshi
import io.dolby.rtscomponentkit.domain.RemoteStreamConfig
import io.dolby.rtscomponentkit.domain.StreamConfig
import io.dolby.rtscomponentkit.domain.StreamConfigList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelChildren

class AminoDeviceRemoteService(private val remoteConfigFlow: RemoteConfigFlow, private val moshi: Moshi) {

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
        try {
            bindService(intent, serviceConnectionListener, Context.BIND_AUTO_CREATE)
        } catch (ex: Exception) {

        }
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
                moshi.adapter(RemoteStreamConfig::class.java).lenient().fromJson(it)
                    ?.let { config ->
                        val streamConfigList = List(config.url.size) { index ->
                            StreamConfig.from(config, index = index)
                        }

                        remoteConfigFlow.updateConfig(StreamConfigList(streamConfigList))
                        Log.d("tvapp.mw_args", remoteConfigFlow.config.value.toString())
                    }
            }
        } catch (e: Exception) {
            remoteConfigFlow.updateConfig(StreamConfigList(emptyList()))
        }
    }

    companion object {
        private const val DEVICE_REMOTE_SERVICE_PACKAGE = "com.aminocom.device"
        private const val DEVICE_REMOTE_SERVICE_CLASS = "com.aminocom.device.DeviceRemoteService"
    }
}
