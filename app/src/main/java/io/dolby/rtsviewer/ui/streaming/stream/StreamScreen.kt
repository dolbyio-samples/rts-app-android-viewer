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
    val lifecycleOwner = rememberUpdatedState(LocalLifecycleOwner.current)
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        Log.i(tag, "Screen subscribe")
        viewModel.onUiAction(StreamAction.CONNECT)
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
                    viewModel.onUiAction(StreamAction.RELEASE)
                }

                else -> {
                }
            }
        }
        val lifecycle = lifecycleOwner.value.lifecycle
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            viewModel.onUiAction(StreamAction.RELEASE)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(16 / 9f)
    ) {
        val context = LocalContext.current
        uiState.streamError?.let {
            ErrorView(error = it)
        } ?: run {
            if (uiState.subscribed) {
                Box(
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    AndroidView(
                        factory = { context ->
                            val view = TextureViewRenderer(context)
                            view.init(Media.eglBaseContext, null)
                            view
                        },
                        update = { view ->
                            view.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT)
                            uiState.videoTrack?.setVideoSink(view)
                        }
                    )
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
