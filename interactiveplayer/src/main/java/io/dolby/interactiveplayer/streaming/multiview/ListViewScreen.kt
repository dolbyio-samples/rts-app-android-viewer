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
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.millicast.VideoRenderer
import io.dolby.interactiveplayer.MainActivity
import io.dolby.interactiveplayer.R
import io.dolby.interactiveplayer.rts.data.MultiStreamingData
import io.dolby.interactiveplayer.rts.data.MultiStreamingData.Companion.video
import io.dolby.interactiveplayer.rts.data.MultiStreamingRepository
import io.dolby.interactiveplayer.rts.ui.DolbyBackgroundBox
import io.dolby.interactiveplayer.rts.ui.LiveIndicator
import io.dolby.interactiveplayer.rts.ui.TopAppBar
import io.dolby.interactiveplayer.streaming.ErrorView
import io.dolby.interactiveplayer.utils.findActivity
import org.webrtc.RendererCommon

@Composable
fun ListViewScreen(
    viewModel: MultiStreamingViewModel = hiltViewModel(),
    onBack: () -> Unit,
    onMainClick: (String?) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val screenContentDescription = stringResource(id = R.string.streaming_screen_contentDescription)

    val focusManager = LocalFocusManager.current
    focusManager.clearFocus()

    Scaffold(
        topBar = {
            TopAppBar(title = uiState.streamName ?: screenContentDescription, onBack = {
                onBack()
                viewModel.disconnect()
            })
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
            uiState.audioTracks.firstOrNull()?.let {
                (context.findActivity() as? MainActivity?)?.addVolumeObserver(it.audioTrack)
            }
            when {
                uiState.error != null -> {
                    ErrorView(error = uiState.error!!)
                }

                uiState.inProgress -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                uiState.videoTracks.isNotEmpty() -> {
                    val configuration = LocalConfiguration.current
                    val onOtherClick = { videoTrack: MultiStreamingData.Video ->
                        viewModel.selectVideoTrack(videoTrack.sourceId)
                    }

                    if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                        HorizontalEndListView(
                            modifier = Modifier,
                            viewModel,
                            uiState,
                            onMainClick,
                            onOtherClick
                        )
                    } else {
                        VerticalTopListView(
                            modifier = Modifier.align(Alignment.Center),
                            viewModel,
                            uiState,
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
    onMainClick: (String?) -> Unit,
    onOtherClick: (MultiStreamingData.Video) -> Unit
) {
    Box(
        modifier = modifier
    ) {
        Row {
            val mainVideo: MultiStreamingData.Video? =
                uiState.videoTracks.firstOrNull { it.sourceId == uiState.selectedVideoTrackId }
                    ?: uiState.videoTracks.firstOrNull()
            Box(
                modifier = Modifier.clickable {
                    onMainClick(uiState.videoTracks.find { it.sourceId == uiState.selectedVideoTrackId }?.id)
                }
            ) {
                AndroidView(
                    modifier = Modifier.aspectRatio(16F / 9),
                    factory = { context ->
                        val view = VideoRenderer(context)
                        view.setZOrderOnTop(true)
                        view.setZOrderMediaOverlay(true)
                        view
                    },
                    update = { view ->
                        view.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT)
                        mainVideo?.play(
                            view = view,
                            viewModel = viewModel,
                            videoQuality = MultiStreamingRepository.VideoQuality.AUTO
                        )
                    },
                    onRelease = {
                        val video =
                            uiState.selectedVideoTrackId?.let { selectedVideoTrackId ->
                                uiState.videoTracks.firstOrNull { it.sourceId == selectedVideoTrackId }
                            } ?: uiState.videoTracks.firstOrNull()
                        video?.let {
                            viewModel.stopVideo(video)
                        }
                        it.release()
                    }
                )
                Text(
                    text = uiState.selectedVideoTrackId ?: "Main",
                    modifier = Modifier.align(
                        Alignment.BottomStart
                    )
                )
                val video =
                    uiState.selectedVideoTrackId?.let { selectedVideoTrackId ->
                        uiState.videoTracks.firstOrNull { it.sourceId == selectedVideoTrackId }
                    } ?: uiState.videoTracks.firstOrNull()
                QualityLabel(
                    viewModel = viewModel,
                    video = video,
                    modifier = Modifier.align(
                        Alignment.BottomEnd
                    )
                )
            }
            val otherTracks =
                uiState.videoTracks.filter { it.sourceId != uiState.selectedVideoTrackId }
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
                        displayLabel = true,
                        videoQuality = MultiStreamingRepository.VideoQuality.LOW,
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
    onMainClick: (String?) -> Unit,
    onOtherClick: (MultiStreamingData.Video) -> Unit
) {
    Box(
        modifier = modifier
    ) {
        val context = LocalContext.current
        if (uiState.audioTracks.isNotEmpty()) {
            (context.findActivity() as? MainActivity?)?.addVolumeObserver(uiState.audioTracks[0].audioTrack)
        }
        Column {
            val selectedVideo = uiState.videoTracks.firstOrNull { it.sourceId == uiState.selectedVideoTrackId }
            val mainVideo: MultiStreamingData.Video? = selectedVideo ?: uiState.videoTracks.firstOrNull()
            Box(
                modifier = Modifier.clickable {
                    onMainClick(uiState.videoTracks.find { it.sourceId == uiState.selectedVideoTrackId }?.id)
                }
            ) {
                AndroidView(
                    modifier = Modifier
                        .aspectRatio(16F / 9),
                    factory = { context ->
                        val view = VideoRenderer(context)
                        view.setZOrderOnTop(true)
                        view.setZOrderMediaOverlay(true)
                        view
                    },
                    update = { view ->
                        view.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT)
                        mainVideo?.play(
                            view = view,
                            viewModel = viewModel,
                            videoQuality = MultiStreamingRepository.VideoQuality.AUTO
                        )
                    },
                    onReset = {
                        mainVideo?.let {
                            viewModel.stopVideo(mainVideo)
                        }
                    }
                )
                Text(
                    text = mainVideo?.sourceId ?: "Main",
                    modifier = Modifier.align(Alignment.BottomStart)
                )
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
                        displayLabel = true,
                        videoQuality = MultiStreamingRepository.VideoQuality.LOW,
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
    video: MultiStreamingData.Video,
    displayLabel: Boolean = true,
    displayQuality: Boolean = false,
    videoQuality: MultiStreamingRepository.VideoQuality = MultiStreamingRepository.VideoQuality.AUTO,
    onClick: ((MultiStreamingData.Video) -> Unit)? = null,
    modifier: Modifier
) {
    val updatedModifier = onClick?.let {
        modifier.clickable { onClick(video) }
    } ?: modifier
    Box {
        AndroidView(
            modifier = updatedModifier,
            factory = { context -> VideoRenderer(context) },
            update = { view ->
                view.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT)
                video.play(view, viewModel, videoQuality)
            },
            onRelease = {
                viewModel.stopVideo(video)
                it.release()
            }
        )
        if (displayLabel) {
            Text(
                text = video.sourceId ?: "Main",
                modifier = Modifier.align(Alignment.BottomStart)
            )
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
fun QualityLabel(
    viewModel: MultiStreamingViewModel,
    video: MultiStreamingData.Video?,
    modifier: Modifier
) {
    val videoQualityState by viewModel.videoQualityState.collectAsStateWithLifecycle()
    Text(
        text = videoQualityState.videoQualities[video?.id]?.name ?: "null",
        modifier = modifier.clickable {
            video?.let {
                viewModel.showVideoQualitySelection(it.id, true)
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

fun MultiStreamingData.Video.play(
    view: VideoRenderer,
    viewModel: MultiStreamingViewModel,
    videoQuality: MultiStreamingRepository.VideoQuality = MultiStreamingRepository.VideoQuality.AUTO
) {
    videoTrack.setRenderer(view)
    viewModel.playVideo(
        this,
        videoQuality
    )
}
