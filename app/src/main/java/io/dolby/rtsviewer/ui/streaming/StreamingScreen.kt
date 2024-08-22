package io.dolby.rtsviewer.ui.streaming

import android.view.KeyEvent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.nativeKeyCode
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.millicast.Media
import com.millicast.video.TextureViewRenderer
import io.dolby.rtscomponentkit.ui.DolbyBackgroundBox
import io.dolby.rtsviewer.MainActivity
import io.dolby.rtsviewer.R
import io.dolby.rtsviewer.utils.KeepScreenOn
import io.dolby.rtsviewer.utils.SetupVolumeControlAudioStream
import io.dolby.rtsviewer.utils.findActivity
import org.webrtc.RendererCommon

@Composable
fun StreamingScreen(viewModel: StreamingViewModel = hiltViewModel(), onBack: () -> Unit) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val showSettings = viewModel.showSettings.collectAsState()
    val showStatistics = viewModel.showStatistics.collectAsState()
    val showSimulcastSettings = viewModel.showSimulcastSettings.collectAsState()
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(showSettings.value, showSimulcastSettings.value) {
        // always focus at the top level so we get key events
        if (!showSettings.value && !showSimulcastSettings.value) {
            focusRequester.requestFocus() // Ensure gaining focus after returning from settings
        }
    }
    val screenContentDescription = stringResource(id = R.string.streaming_screen_contentDescription)
    DolbyBackgroundBox(
        modifier = Modifier
            .semantics {
                contentDescription = screenContentDescription
            }
            .focusRequester(focusRequester)
            .onKeyEvent {
                if (it.type == KeyEventType.KeyUp && (it.key.nativeKeyCode == KeyEvent.KEYCODE_PAGE_DOWN || it.key.nativeKeyCode == KeyEvent.KEYCODE_CHANNEL_DOWN)) {
                    viewModel.switchChannel(ChannelNavDirection.DOWN)
                    return@onKeyEvent true
                } else if (it.type == KeyEventType.KeyUp && (it.key.nativeKeyCode == KeyEvent.KEYCODE_PAGE_UP || it.key.nativeKeyCode == KeyEvent.KEYCODE_CHANNEL_UP)) {
                    viewModel.switchChannel(ChannelNavDirection.UP)
                    return@onKeyEvent true
                }
                return@onKeyEvent false
            }
            .clickable {
                viewModel.switchChannel(ChannelNavDirection.DOWN)
            }
            .focusable()
    ) {
        val context = LocalContext.current
        when {
            uiState.error != null -> {
                ErrorView(error = uiState.error!!)
            }
            uiState.subscribed && uiState.error == null -> {
                Box(
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    AndroidView(
                        factory = { context ->
                            val view = TextureViewRenderer(context)
                            view.init(Media.eglBaseContext, null)
                            uiState.videoTrack?.setVideoSink(view)
                            view
                        },
                        update = { view ->
                            view.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT)
                        },
                        onRelease = {
                            uiState.videoTrack?.removeVideoSink(it)
                            it.release()
                        }
                    )
                    SetupVolumeControlAudioStream()
                }
            }
            uiState.disconnected -> {
                (context.findActivity() as? MainActivity?)?.unregisterVolumeObserverIfExists()
            }
        }

        uiState.audioTrack?.let {
            (context.findActivity() as? MainActivity?)?.addVolumeObserver(it)
        }

        if (uiState.subscribed) {
            KeepScreenOn(enabled = true)
        } else {
            KeepScreenOn(enabled = false)
        }

        StreamingToolbarView(viewModel = viewModel)

        if (showSimulcastSettings.value) {
            SimulcastScreen(viewModel)
        } else if (showSettings.value) {
            SettingsScreen(viewModel)
        }

        if (showStatistics.value && uiState.subscribed) {
            StatisticsView(
                viewModel = viewModel,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(horizontal = 22.dp, vertical = 15.dp)
            )
        }

        BackHandler {
            if (showSimulcastSettings.value) {
                viewModel.updateShowSimulcastSettings(false)
            } else if (showSettings.value) {
                viewModel.settingsVisibility(false)
            } else {
                onBack.invoke()
                viewModel.clear()
            }
        }
    }
}
