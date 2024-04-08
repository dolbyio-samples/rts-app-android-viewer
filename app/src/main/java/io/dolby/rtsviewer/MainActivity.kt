package io.dolby.rtsviewer

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.compose.rememberNavController
import com.millicast.subscribers.remote.RemoteAudioTrack
import dagger.hilt.android.AndroidEntryPoint
import io.dolby.rtscomponentkit.utils.RemoteVolumeObserver
import io.dolby.rtsviewer.ui.navigation.AppNavigation
import io.dolby.rtsviewer.uikit.theme.RTSViewerTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private var volumeObserver: RemoteVolumeObserver? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val navController = rememberNavController()
            RTSViewerTheme {
                AppNavigation(navController)
            }
        }
    }

    override fun onDestroy() {
        unregisterVolumeObserverIfExists()
        super.onDestroy()
    }

    fun addVolumeObserver(audioTrack: RemoteAudioTrack) {
        unregisterVolumeObserverIfExists()
        volumeObserver = RemoteVolumeObserver(this, Handler(Looper.getMainLooper()), audioTrack)

        volumeObserver?.let {
            contentResolver.registerContentObserver(
                Settings.System.CONTENT_URI,
                true,
                it
            )
        }
    }

    fun unregisterVolumeObserverIfExists() {
        volumeObserver?.let {
            applicationContext.contentResolver.unregisterContentObserver(it)
        }
    }
}
