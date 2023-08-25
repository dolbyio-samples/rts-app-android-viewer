package io.dolby.rtsviewer

import android.media.AudioManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.compose.rememberNavController
import com.millicast.AudioTrack
import dagger.hilt.android.AndroidEntryPoint
import io.dolby.rtscomponentkit.utils.VolumeObserver
import io.dolby.rtsviewer.ui.navigation.AppNavigation
import io.dolby.rtsviewer.uikit.theme.RTSViewerTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private var volumeObserver: VolumeObserver? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val navController = rememberNavController()
            RTSViewerTheme {
                AppNavigation(navController)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        //volumeControlStream = AudioManager.STREAM_MUSIC
        val am = getSystemService(AudioManager::class.java) as AudioManager
        am.mode = AudioManager.MODE_IN_COMMUNICATION
        am.isSpeakerphoneOn = true
    }

    override fun onDestroy() {
        unregisterVolumeObserverIfExists()
        super.onDestroy()
    }

    fun addVolumeObserver(audioTrack: AudioTrack) {
        unregisterVolumeObserverIfExists()
        volumeObserver = VolumeObserver(this, Handler(Looper.getMainLooper()), audioTrack)

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
