package io.dolby.rtsviewer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.dolby.uicomponents.ui.theme.RTSViewerTheme
import dagger.hilt.android.AndroidEntryPoint
import io.dolby.rtsviewer.ui.navigation.AppNavigation
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RTSViewerTheme {
                AppNavigation()
            }
        }
    }
}
