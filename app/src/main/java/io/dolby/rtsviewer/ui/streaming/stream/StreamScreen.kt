package io.dolby.rtsviewer.ui.streaming.stream

import android.content.Context
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Text
import com.millicast.Media
import com.millicast.video.TextureViewRenderer
import io.dolby.rtscomponentkit.domain.StreamConfig
import io.dolby.rtsviewer.ui.streaming.common.ErrorView
import io.dolby.rtsviewer.ui.streaming.common.LiveIndicatorView
import io.dolby.rtsviewer.uikit.button.StyledIconButton
import io.dolby.uikit.R
import org.webrtc.RendererCommon
import org.webrtc.VideoFrame

class LogTextureViewRenderer(context: Context) : TextureViewRenderer(context) {
    override fun onFrame(videoFrame: VideoFrame?) {
        super.onFrame(videoFrame)

        Log.d("LogTextureViewRenderer", "onFrame")
    }
}

@Composable
fun StreamScreen(streamInfo: StreamConfig) {
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
            val events = object : RendererCommon.RendererEvents {
                override fun onFirstFrameRendered() {
                    Log.d(tag, "${streamInfo.index} onFirstFrameRendered")
                }

                override fun onFrameResolutionChanged(p0: Int, p1: Int, p2: Int) {
                    Log.d(tag, "${streamInfo.index} onFrameResolutionChanged")
                }

            }
            Log.d(tag, "${streamInfo.index} TextureViewRenderer init")
            init(Media.eglBaseContext, events)
        }
    }

    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        if (uiState.shouldRequestFocusInitially) {
            focusRequester.requestFocus()
        }
        viewModel.onUiAction(StreamAction.Connect)
    }

    val borderColor =
        if (uiState.isFocused && !uiState.isSingleStreamView) MaterialTheme.colors.primaryVariant else Color.Transparent
    val borderWidth =
        if (uiState.isFocused && !uiState.isSingleStreamView) 5.dp else 0.dp

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focusRequester)
            .onFocusChanged {
                viewModel.onUiAction(StreamAction.UpdateFocus(it.isFocused))
            }
            .focusable()
            .clickable {
                viewModel.onUiAction(
                    StreamAction.UpdateSettingsVisibility(true)
                )
            }
            .border(borderWidth, borderColor)
            .aspectRatio(16 / 9f)
    ) {

        DisposableEffect(viewModel) {
            onDispose {
                Log.d(tag, "${streamInfo.index} Video Track Release")
                viewModel.onUiAction(StreamAction.Release)
            }
        }

        if (uiState.videoTrack != null) {
            DisposableEffect(uiState.videoTrack) {
                val lifecycle = lifecycleOwner.value.lifecycle
                val observer = LifecycleEventObserver { _, event ->
                    when (event) {
                        Lifecycle.Event.ON_PAUSE -> {
                            Log.d(tag, "${streamInfo.index} Video Track Pause")
                            viewModel.onUiAction(StreamAction.Pause)
                        }

                        Lifecycle.Event.ON_RESUME -> {
                            Log.d(tag, "${streamInfo.index} Video Track Play")
                            viewModel.onUiAction(StreamAction.Play(videoRenderer))
                        }

                        else -> {}
                    }
                }
                lifecycle.addObserver(observer)
                onDispose {
                    Log.d(tag, "${streamInfo.index} onDispose")
                    lifecycle.removeObserver(observer)
                }
            }
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxSize()
                    .alpha(if (uiState.streamError == null) 1.0f else 0.0f)
            ) {
                AndroidView(
                    factory = { videoRenderer },
                    update = { view ->
                        view.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT)
                    },
                    onRelease = { videoRenderer.release() }
                )
            }

            val toolbarContentDescription =
                stringResource(id = io.dolby.rtsviewer.R.string.streamingToolbar_contentDescription)
            ConstraintLayout(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(if (uiState.streamError == null) 1.0f else 0.0f)
                    .semantics { contentDescription = toolbarContentDescription }
            ) {
                val (toolbar, settings, stats, liveIndicator) = createRefs()
                AnimatedVisibility(
                    visible = uiState.showSettingsButton,
                    modifier = Modifier
                        .constrainAs(toolbar) {
                            bottom.linkTo(parent.bottom)
                            end.linkTo(parent.end)
                        }
                        .semantics { contentDescription = "visibility button" }
                ) {
                    StyledIconButton(
                        modifier = Modifier
                            .constrainAs(settings) {
                                bottom.linkTo(parent.bottom, margin = 5.dp)
                                end.linkTo(parent.end, margin = 5.dp)
                            },
                        icon = painterResource(id = R.drawable.ic_settings),
                        text = if (uiState.isSingleStreamView) stringResource(id = io.dolby.rtsviewer.R.string.settings_title) else null
                    )
                }

                if (uiState.showStatistics) {
                    StatisticsScreen(
                        viewModel = viewModel,
                        modifier = Modifier
                            .constrainAs(stats) {
                                bottom.linkTo(parent.bottom, margin = 5.dp)
                                absoluteLeft.linkTo(parent.absoluteLeft, margin = 5.dp)
                                absoluteRight.linkTo(settings.absoluteLeft, margin = 10.dp)
                            },
                    )
                }

                if (uiState.shouldShowLiveIndicator) {
                    LiveIndicatorView(
                        modifier = Modifier
                            .constrainAs(liveIndicator) {
                                top.linkTo(parent.top, margin = 10.dp)
                                absoluteLeft.linkTo(parent.absoluteLeft, margin = 10.dp)
                            },
                        isLive = uiState.subscribed
                    )
                }
            }
        } else {
            Text(text = "Please wait....", color = MaterialTheme.colors.onSurface)
        }

        uiState.streamError?.let {
            ErrorView(error = it)
        }
    }
}

