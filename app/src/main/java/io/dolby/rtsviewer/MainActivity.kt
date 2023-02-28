package io.dolby.rtsviewer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.dolby.uicomponents.ui.theme.RTSViewerTheme
import io.dolby.rtsviewer.ui.navigation.AppNavigation

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
