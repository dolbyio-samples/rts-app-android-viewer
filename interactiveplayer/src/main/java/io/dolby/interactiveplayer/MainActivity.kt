package io.dolby.interactiveplayer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import io.dolby.interactiveplayer.navigation.AppNavigation
import io.dolby.interactiveplayer.utils.SetupVolumeControlAudioStream
import io.dolby.rtsviewer.uikit.theme.RTSViewerTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val navController = rememberNavController()
            RTSViewerTheme {
                SetupVolumeControlAudioStream()
                AppNavigation(navController)
            }
        }
    }
}
