package io.dolby.rtsviewer.ui.streaming

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.millicast.VideoRenderer
import io.dolby.rtscomponentkit.ui.DolbyBackgroundBox
import io.dolby.rtscomponentkit.ui.TopAppBar
import io.dolby.rtsviewer.MainActivity
import io.dolby.rtsviewer.R
import io.dolby.rtsviewer.utils.KeepScreenOn
import io.dolby.rtsviewer.utils.SetupVolumeControlAudioStream
import io.dolby.rtsviewer.utils.findActivity
import org.webrtc.RendererCommon

@Composable
fun ListViewScreen(viewModel: MultiStreamingViewModel = hiltViewModel(), onBack: () -> Unit) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val screenContentDescription = stringResource(id = R.string.streaming_screen_contentDescription)

    Scaffold(
        topBar = {
            TopAppBar(title = "Stream") {
                onBack()
            }
        }
    ) { paddingValues ->
        DolbyBackgroundBox(
            modifier = Modifier
                .semantics {
                    contentDescription = screenContentDescription
                }
                .padding(paddingValues)
        ) {
            val context = LocalContext.current
            when {
                uiState.error != null -> {
                    ErrorView(error = uiState.error!!)
                }
                uiState.inProgress -> {
                    CircularProgressIndicator()
                }
                uiState.videoTracks.isNotEmpty() -> {
                    Box(
                        modifier = Modifier.align(Alignment.Center)
                    ) {
                        AndroidView(
                            factory = { context -> VideoRenderer(context) },
                            update = { view ->
                                view.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT)
                                uiState.videoTracks.firstOrNull()?.videoTrack?.setRenderer(view)
                            }
                        )
                        SetupVolumeControlAudioStream()
                    }
                }
            }
        }
    }
}
