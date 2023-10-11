package io.dolby.interactiveplayer

import android.media.AudioManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import io.dolby.interactiveplayer.navigation.AppNavigation
import io.dolby.rtsviewer.uikit.theme.RTSViewerTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
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
        val am = getSystemService(AudioManager::class.java) as AudioManager
        am.mode = AudioManager.MODE_IN_COMMUNICATION
        am.isSpeakerphoneOn = true
    }
}
