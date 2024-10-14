package io.dolby.rtsviewer.ui.streaming.stream

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.millicast.Media
import com.millicast.video.TextureViewRenderer
import io.dolby.rtsviewer.ui.streaming.common.ErrorView
import io.dolby.rtsviewer.ui.streaming.common.StreamInfo
import org.webrtc.RendererCommon

@Composable
fun StreamScreen(streamInfo: StreamInfo) {
    val viewModel = hiltViewModel(
        key = streamInfo.index.toString(),
        creationCallback = { factory: StreamViewModel.Factory ->
            factory.create(streamInfo)
        }
    )
    val tag = "StreamScreen - ${streamInfo.index}"
    val context = LocalContext.current
    val lifecycleOwner = rememberUpdatedState(LocalLifecycleOwner.current)
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val videoRenderer = remember(context) {
        TextureViewRenderer(context).apply {
            init(Media.eglBaseContext, null)
        }
    }

    LaunchedEffect(Unit) {
        Log.i(tag, "Screen subscribe")
        viewModel.onUiAction(StreamAction.Connect)
    }

    DisposableEffect(viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    Log.d(tag, " Lifecycle onPause")
                }

                Lifecycle.Event.ON_RESUME -> {
                    Log.d(tag, "Lifecycle OnResume")
                }

                Lifecycle.Event.ON_DESTROY -> {
                    Log.d(tag, "Lifecycle onDestroy")
                    viewModel.onUiAction(StreamAction.Release)
                }

                else -> {
                }
            }
        }
        val lifecycle = lifecycleOwner.value.lifecycle
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            viewModel.onUiAction(StreamAction.Release)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(16 / 9f)
    ) {
        uiState.streamError?.let {
            ErrorView(error = it)
        } ?: run {
            if (uiState.subscribed) {
                Box(
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    AndroidView(
                        factory = { videoRenderer },
                        update = { view ->
                            view.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT)
                        }
                    )
                }
                uiState.videoTrack?.let {
                    DisposableEffect(uiState.videoTrack, uiState.selectedStreamQuality) {
                        val observer = LifecycleEventObserver { _, event ->
                            when (event) {
                                Lifecycle.Event.ON_PAUSE -> {
                                    viewModel.onUiAction(StreamAction.Pause)
                                }

                                Lifecycle.Event.ON_RESUME -> {
                                    viewModel.onUiAction(StreamAction.Play(videoRenderer))
                                }
                                else -> {}
                            }
                        }
                        val lifecycle = lifecycleOwner.value.lifecycle
                        lifecycle.addObserver(observer)
                        onDispose {
                            lifecycle.removeObserver(observer)
                            viewModel.onUiAction(StreamAction.Pause)
                        }
                    }
                }
            }
        }

        if (uiState.showStatistics) {
            StatisticsScreen(
                viewModel = viewModel,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(horizontal = 22.dp, vertical = 15.dp)
            )
        }
    }
}
