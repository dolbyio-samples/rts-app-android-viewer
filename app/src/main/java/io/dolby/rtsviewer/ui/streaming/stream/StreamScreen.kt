package io.dolby.rtsviewer.ui.streaming.stream

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.millicast.Media
import com.millicast.video.TextureViewRenderer
import io.dolby.rtscomponentkit.domain.StreamConfig
import io.dolby.rtsviewer.ui.streaming.common.ErrorView
import io.dolby.rtsviewer.uikit.button.StyledIconButton
import io.dolby.uikit.R
import org.webrtc.RendererCommon

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
            init(Media.eglBaseContext, null)
        }
    }

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        Log.i(tag, "Screen subscribe")
        if (uiState.shouldRequestFocusInitially) {
            focusRequester.requestFocus()
        }
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
    val borderColor =
        if (uiState.isFocused) MaterialTheme.colors.primaryVariant else Color.Transparent
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
            .border(5.dp, borderColor)
            .aspectRatio(16 / 9f)
    ) {
        uiState.streamError?.let {
            ErrorView(error = it)
        } ?: run {
            if (uiState.subscribed) {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                ) {
                    AndroidView(
                        factory = { videoRenderer },
                        update = { view ->
                            view.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT)
                        }
                    )
                }
                DisposableEffect(uiState.videoTrack, uiState.selectedStreamQuality) {
                    val observer = LifecycleEventObserver { _, event ->
                        when (event) {
                            Lifecycle.Event.ON_PAUSE -> {
                                viewModel.onUiAction(StreamAction.Pause)
                            }

                            Lifecycle.Event.ON_RESUME -> {
                                Log.d(tag, "Video Track Play")
                                viewModel.onUiAction(StreamAction.Play(videoRenderer))
                            }

                            else -> {}
                        }
                    }
                    val lifecycle = lifecycleOwner.value.lifecycle
                    lifecycle.addObserver(observer)
                    onDispose {
                        Log.d(tag, "onDispose")
                        lifecycle.removeObserver(observer)
                        viewModel.onUiAction(StreamAction.Pause)
                    }
                }

                val toolbarContentDescription =
                    stringResource(id = io.dolby.rtsviewer.R.string.streamingToolbar_contentDescription)
                ConstraintLayout(
                    modifier = Modifier
                        .fillMaxSize()
                        .semantics { contentDescription = toolbarContentDescription }
                ) {
                    val (toolbar, settings) = createRefs()
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
                                    bottom.linkTo(parent.bottom, margin = 14.dp)
                                    end.linkTo(parent.end, margin = 20.dp)
                                },
                            icon = painterResource(id = R.drawable.ic_settings),
                            text = stringResource(id = io.dolby.rtsviewer.R.string.settings_title)
                        )
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
