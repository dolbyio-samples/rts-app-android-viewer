package io.dolby.interactiveplayer.streaming.multiview

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.millicast.Media
import com.millicast.subscribers.remote.RemoteAudioTrack
import com.millicast.subscribers.remote.RemoteVideoTrack
import com.millicast.video.TextureViewRenderer
import io.dolby.interactiveplayer.R
import io.dolby.interactiveplayer.rts.ui.DolbyBackgroundBox
import io.dolby.interactiveplayer.rts.ui.LabelIndicator
import io.dolby.interactiveplayer.rts.ui.LiveIndicator
import io.dolby.interactiveplayer.rts.ui.TopAppBar
import io.dolby.interactiveplayer.streaming.ErrorView
import io.dolby.rtscomponentkit.data.multistream.VideoQuality
import io.dolby.rtsviewer.uikit.text.Text
import org.webrtc.RendererCommon
import org.webrtc.VideoSink

@Composable
fun ListViewScreen(
    viewModel: MultiStreamingViewModel,
    onBack: () -> Unit,
    onMainClick: (String?) -> Unit,
    onSettingsClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val screenContentDescription = stringResource(id = R.string.streaming_screen_contentDescription)

    val focusManager = LocalFocusManager.current
    focusManager.clearFocus()

    val showSourceLabels = viewModel.showSourceLabels.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = uiState.streamName ?: screenContentDescription,
                onBack = {
                    viewModel.disconnect()
                    onBack()
                },
                onAction = onSettingsClick
            )
        }
    ) { paddingValues ->
        DolbyBackgroundBox(
            modifier = Modifier
                .semantics {
                    contentDescription = screenContentDescription
                }
                .padding(paddingValues)
        ) {
            when {
                uiState.error != null -> {
                    ErrorView(error = uiState.error!!)
                }

                uiState.inProgress -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                uiState.videoTracks.isNotEmpty() -> {
                    val configuration = LocalConfiguration.current
                    val onOtherClick = { videoTrack: RemoteVideoTrack ->
                        viewModel.selectVideoTrack(videoTrack.sourceId)
                    }

                    if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                        HorizontalEndListView(
                            modifier = Modifier,
                            viewModel,
                            uiState,
                            showSourceLabels.value,
                            onMainClick,
                            onOtherClick
                        )
                    } else {
                        VerticalTopListView(
                            modifier = Modifier.align(Alignment.Center),
                            viewModel,
                            uiState,
                            showSourceLabels.value,
                            onMainClick,
                            onOtherClick
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HorizontalEndListView(
    modifier: Modifier,
    viewModel: MultiStreamingViewModel,
    uiState: MultiStreamingUiState,
    displayLabel: Boolean,
    onMainClick: (String?) -> Unit,
    onOtherClick: (RemoteVideoTrack) -> Unit
) {
    Box(
        modifier = modifier
    ) {
        Row {
            val selectedVideo =
                uiState.videoTracks.firstOrNull { it.sourceId == uiState.selectedVideoTrackId }
            val mainVideo: RemoteVideoTrack? =
                selectedVideo ?: uiState.videoTracks.firstOrNull()?.also {
                    viewModel.selectVideoTrack(it.sourceId)
                }
            Box(
                modifier = Modifier.clickable {
                    onMainClick(uiState.videoTracks.find { it.sourceId == uiState.selectedVideoTrackId }?.currentMid)
                }
            ) {
                mainVideo?.let {
                    VideoView(
                        viewModel = viewModel,
                        video = it,
                        displayLabel = displayLabel,
                        videoQuality = VideoQuality.LOW,
                        onClick = { onMainClick(uiState.videoTracks.find { it.sourceId == uiState.selectedVideoTrackId }?.currentMid) },
                        modifier = Modifier.aspectRatio(16F / 9)
                    )
                }
            }
            val otherTracks =
                uiState.videoTracks.filter { it.sourceId != mainVideo?.sourceId }

            LazyColumn(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(start = 5.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                items(otherTracks) { video ->
                    VideoView(
                        viewModel = viewModel,
                        video = video,
                        displayLabel = displayLabel,
                        videoQuality = VideoQuality.LOW,
                        onClick = onOtherClick,
                        modifier = Modifier.aspectRatio(16F / 9)
                    )
                }
            }
        }
        LiveIndicatorComponent(
            modifier = Modifier.align(Alignment.TopStart),
            on = uiState.videoTracks.isNotEmpty() || uiState.audioTracks.isNotEmpty()
        )
    }
}

@Composable
fun VerticalTopListView(
    modifier: Modifier,
    viewModel: MultiStreamingViewModel,
    uiState: MultiStreamingUiState,
    displayLabel: Boolean,
    onMainClick: (String?) -> Unit,
    onOtherClick: (RemoteVideoTrack) -> Unit
) {
    Box(
        modifier = modifier
    ) {
        Column {
            val selectedVideo =
                uiState.videoTracks.firstOrNull { it.sourceId == uiState.selectedVideoTrackId }
            val mainVideo: RemoteVideoTrack? =
                selectedVideo ?: uiState.videoTracks.firstOrNull()?.also {
                    viewModel.selectVideoTrack(it.sourceId)
                }
            Box(
                modifier = Modifier.clickable {
                    onMainClick(uiState.videoTracks.find { it.sourceId == uiState.selectedVideoTrackId }?.currentMid)
                }
            ) {
                mainVideo?.let {
                    VideoView(
                        viewModel = viewModel,
                        video = it,
                        displayLabel = displayLabel,
                        videoQuality = VideoQuality.LOW,
                        onClick = { onMainClick(uiState.videoTracks.find { it.sourceId == uiState.selectedVideoTrackId }?.currentMid) },
                        modifier = Modifier.aspectRatio(16F / 9)
                    )
                }
//                QualityLabel(
//                    viewModel = viewModel,
//                    video = mainVideo,
//                    modifier = Modifier.align(Alignment.BottomEnd)
//                )
            }
            val otherTracks =
                uiState.videoTracks.filter { it.sourceId != mainVideo?.sourceId }

            val lazyVerticalGridState = rememberLazyGridState()

            LazyVerticalGrid(
                state = lazyVerticalGridState,
                columns = GridCells.Fixed(count = 2),
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(top = 5.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                items(items = otherTracks) { video ->
                    VideoView(
                        viewModel = viewModel,
                        video = video,
                        displayLabel = displayLabel,
                        videoQuality = VideoQuality.LOW,
                        onClick = onOtherClick,
                        modifier = Modifier.aspectRatio(16F / 9)
                    )
                }
            }
        }
        LiveIndicatorComponent(
            modifier = Modifier.align(Alignment.TopStart),
            on = uiState.videoTracks.isNotEmpty() || uiState.audioTracks.isNotEmpty()
        )

        QualitySelector(viewModel = viewModel)
    }
}

@Composable
fun VideoView(
    viewModel: MultiStreamingViewModel,
    video: RemoteVideoTrack,
    displayLabel: Boolean = true,
    displayQuality: Boolean = false,
    videoQuality: VideoQuality = VideoQuality.AUTO,
    onClick: ((RemoteVideoTrack) -> Unit)? = null,
    modifier: Modifier
) {
    val updatedModifier = onClick?.let {
        modifier.clickable { onClick(video) }
    } ?: modifier
    val context = LocalContext.current
    val videoRenderer = remember(context) {
        TextureViewRenderer(context).apply {
            init(Media.eglBaseContext, null)
        }
    }
    Box {
        AndroidView(
            modifier = updatedModifier,
            factory = { videoRenderer },
            update = { view ->
                view.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT)
            }
        )
        VideoTrackLifecycleObserver(video = video, videoSink = videoRenderer)
        if (displayLabel) {
            LabelIndicator(modifier = Modifier.align(Alignment.BottomStart), label = video.sourceId)
        }
        if (displayQuality) {
            QualityLabel(
                viewModel = viewModel,
                video = video,
                modifier = Modifier.align(Alignment.BottomEnd)
            )
        }
    }
}

@Composable
fun VideoTrackLifecycleObserver(video: RemoteVideoTrack, videoSink: VideoSink) {
    val lifecycleOwner = rememberUpdatedState(LocalLifecycleOwner.current)
    DisposableEffect(video) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    video.disableAsync()
                }

                Lifecycle.Event.ON_RESUME -> {
                    video.enableAsync(videoSink = videoSink)
                }
                else -> {
                }
            }
        }
        val lifecycle = lifecycleOwner.value.lifecycle
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            video.disableAsync()
        }
    }
}

@Composable
fun AudioTrackLifecycleObserver(audioTrack: RemoteAudioTrack?) {
    audioTrack?.let { audio ->
        val lifecycleOwner = rememberUpdatedState(LocalLifecycleOwner.current)
        DisposableEffect(audioTrack) {
            val observer = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_PAUSE -> {
                        audio.disableAsync()
                    }
                    Lifecycle.Event.ON_RESUME -> {
                        audio.enableAsync()
                    }

                    else -> {
                    }
                }
            }
            val lifecycle = lifecycleOwner.value.lifecycle
            lifecycle.addObserver(observer)
            onDispose {
                lifecycle.removeObserver(observer)
                audio.disableAsync()
            }
        }
    }
}

@Composable
fun QualityLabel(
    viewModel: MultiStreamingViewModel,
    video: RemoteVideoTrack?,
    modifier: Modifier
) {
    val videoQualityState by viewModel.videoQualityState.collectAsStateWithLifecycle()
    Text(
        text = videoQualityState.videoQualities[video?.currentMid]?.name ?: "null",
        modifier = modifier.clickable {
            video?.let {
                viewModel.showVideoQualitySelection(it.currentMid, true)
            }
        }
    )
}

@Composable
fun QualitySelector(
    viewModel: MultiStreamingViewModel
) {
    val videoQualityState by viewModel.videoQualityState.collectAsStateWithLifecycle()
    videoQualityState.showVideoQualitySelectionForMid?.let { mid ->
        val availableVideoQualitiesForStream =
            videoQualityState.availableVideoQualities[mid]?.map { it.videoQuality() } ?: emptyList()
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable { viewModel.showVideoQualitySelection(null, false) }
        ) {
            LazyColumn(
                modifier = Modifier
                    .align(Alignment.Center)
                    .width(200.dp)
                    .background(Color.Black)
                    .padding(5.dp)
            ) {
                item {
                    Text(
                        text = "Select from available Layers, mid = $mid",
                        Modifier.clickable { viewModel.showVideoQualitySelection(null, false) }
                    )
                }
                items(items = availableVideoQualitiesForStream) {
                    Text(
                        text = it.name,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(3.dp)
                            .clickable {
                                viewModel.preferredVideoQuality(mid = mid, videoQuality = it)
                                viewModel.showVideoQualitySelection(null, false)
                            }
                    )
                }
            }
        }
    }
}

@Composable
fun LiveIndicatorComponent(modifier: Modifier, on: Boolean) {
    LiveIndicator(
        modifier = modifier,
        on = on
    )
}
