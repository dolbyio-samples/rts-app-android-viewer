package io.dolby.rtsviewer.ui.streaming.multiview

import android.content.res.Configuration
import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
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
import io.dolby.rtscomponentkit.data.MultiStreamingData
import io.dolby.rtscomponentkit.data.MultiStreamingRepository
import io.dolby.rtscomponentkit.ui.DolbyBackgroundBox
import io.dolby.rtscomponentkit.ui.LiveIndicator
import io.dolby.rtscomponentkit.ui.TopAppBar
import io.dolby.rtsviewer.MainActivity
import io.dolby.rtsviewer.R
import io.dolby.rtsviewer.ui.streaming.ErrorView
import io.dolby.rtsviewer.utils.SetupVolumeControlAudioStream
import io.dolby.rtsviewer.utils.findActivity
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
            TopAppBar(title = uiState.streamName ?: screenContentDescription) {
                onBack()
                viewModel.disconnect()
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
        SetupVolumeControlAudioStream()
        Row {
            Box(modifier = Modifier.clickable {
                onMainClick(uiState.videoTracks.find { it.sourceId == uiState.selectedVideoTrackId }?.id)
            }) {
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
                        val video =
                            uiState.selectedVideoTrackId?.let { selectedVideoTrackId ->
                                uiState.videoTracks.firstOrNull { it.sourceId == selectedVideoTrackId }
                            } ?: uiState.videoTracks.firstOrNull()
                        video?.play(view, viewModel)
                    },
                    onRelease = {
                        val video =
                            uiState.selectedVideoTrackId?.let { selectedVideoTrackId ->
                                uiState.videoTracks.firstOrNull { it.sourceId == selectedVideoTrackId }
                            } ?: uiState.videoTracks.firstOrNull()
                        video?.let {
                            viewModel.stopVideo(video)
                        }
                    }
                )
                Text(
                    text = uiState.selectedVideoTrackId ?: "Main",
                    modifier = Modifier.align(
                        Alignment.BottomStart
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
                    VideoView(onClick = onOtherClick, video = video, viewModel = viewModel)
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
        SetupVolumeControlAudioStream()
        val context = LocalContext.current
        if (uiState.audioTracks.isNotEmpty()) {
            (context.findActivity() as? MainActivity?)?.addVolumeObserver(uiState.audioTracks[0].audioTrack)
        }
        Column {
            Box(modifier = Modifier.clickable {
                onMainClick(uiState.videoTracks.find { it.sourceId == uiState.selectedVideoTrackId }?.id)
            }) {
                val video =
                    uiState.selectedVideoTrackId?.let { selectedVideoTrackId ->
                        uiState.videoTracks.firstOrNull { it.sourceId == selectedVideoTrackId }
                    } ?: uiState.videoTracks.firstOrNull()
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

                        video?.play(view, viewModel)
                    },
                    onReset = {
                        video?.let {
                            viewModel.stopVideo(video)
                        }
                    }
                )
                Text(
                    text = uiState.selectedVideoTrackId ?: "Main",
                    modifier = Modifier.align(
                        Alignment.BottomStart
                    )
                )
            }
            val otherTracks =
                uiState.videoTracks.filter { it.sourceId != uiState.selectedVideoTrackId }
            val lazyVerticalGridState = rememberLazyGridState()
            LazyVerticalGrid(
                state = lazyVerticalGridState,
                columns = GridCells.Fixed(count = 2),
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(top = 5.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                items(items = otherTracks) { video ->
                    VideoView(onOtherClick, video, viewModel)
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
private fun VideoView(
    onClick: (MultiStreamingData.Video) -> Unit,
    video: MultiStreamingData.Video,
    viewModel: MultiStreamingViewModel
) {
    Box {
        AndroidView(
            modifier = Modifier
                .aspectRatio(16F / 9)
                .clickable { onClick(video) },
            factory = { context -> VideoRenderer(context) },
            update = { view ->
                Log.d("TAG", "*****> update item ${video.sourceId}, $view")
                view.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT)
                video.play(view, viewModel)
            },
            onReset = {
                Log.d("TAG", "*****> onReset item ${video.sourceId}, $it")
                viewModel.stopVideo(video)
            }
        )
        Text(
            text = video.sourceId ?: "Main",
            modifier = Modifier.align(Alignment.BottomStart)
        )
    }
}

@Composable
fun LiveIndicatorComponent(modifier: Modifier, on: Boolean) {
    LiveIndicator(
        modifier = modifier,
        on = on
    )
}

private fun MultiStreamingData.Video.play(
    view: VideoRenderer,
    viewModel: MultiStreamingViewModel,
    videoQuality: MultiStreamingRepository.VideoQuality = MultiStreamingRepository.VideoQuality.AUTO
) {
    videoTrack.setRenderer(view)
    viewModel.playVideo(
        this,
        videoQuality
    )
    Log.d("TAG", "*****> playVideo: ${this.sourceId}")

}

