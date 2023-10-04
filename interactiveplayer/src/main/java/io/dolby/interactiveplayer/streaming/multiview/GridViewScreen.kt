package io.dolby.interactiveplayer.streaming.multiview

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import io.dolby.interactiveplayer.MainActivity
import io.dolby.interactiveplayer.R
import io.dolby.interactiveplayer.rts.data.MultiStreamingData
import io.dolby.interactiveplayer.rts.data.MultiStreamingRepository
import io.dolby.interactiveplayer.rts.ui.DolbyBackgroundBox
import io.dolby.interactiveplayer.rts.ui.TopAppBar
import io.dolby.interactiveplayer.streaming.ErrorView
import io.dolby.interactiveplayer.utils.findActivity
import org.webrtc.RendererCommon

@Composable
fun GridViewScreen(
    viewModel: MultiStreamingViewModel = hiltViewModel(),
    onBack: () -> Unit,
    onMainClick: (String?) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val screenContentDescription = stringResource(id = R.string.streaming_screen_contentDescription)

    val focusManager = LocalFocusManager.current
    focusManager.clearFocus()
    val showSourceLabels = viewModel.showSourceLabels.collectAsState()

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
                    val columnCount =
                        if (LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE) 4 else 2
                    Grid(
                        modifier = Modifier.align(Alignment.Center),
                        columnCount = columnCount,
                        showSourceLabels = showSourceLabels.value,
                        viewModel,
                        uiState,
                        onMainClick
                    )
                }
            }
        }
    }
}

@Composable
private fun Grid(
    modifier: Modifier,
    columnCount: Int = 2,
    showSourceLabels: Boolean,
    viewModel: MultiStreamingViewModel,
    uiState: MultiStreamingUiState,
    onMainClick: (String?) -> Unit
) {
    Box(
        modifier = modifier
    ) {
        val context = LocalContext.current
        if (uiState.audioTracks.isNotEmpty()) {
            (context.findActivity() as? MainActivity?)?.addVolumeObserver(uiState.audioTracks[0].audioTrack)
        }
        val otherTracks = uiState.videoTracks
        val lazyVerticalGridState = rememberLazyGridState()
        LazyVerticalGrid(
            state = lazyVerticalGridState,
            columns = GridCells.Fixed(count = columnCount),
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
                    displayLabel = showSourceLabels,
                    videoQuality = MultiStreamingRepository.VideoQuality.LOW,
                    onClick = onMainClick,
                    modifier = Modifier.aspectRatio(16F / 9)
                )
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
private fun VideoView(
    viewModel: MultiStreamingViewModel,
    video: MultiStreamingData.Video,
    displayLabel: Boolean = true,
    displayQuality: Boolean = false,
    videoQuality: MultiStreamingRepository.VideoQuality = MultiStreamingRepository.VideoQuality.AUTO,
    onClick: ((String?) -> Unit)? = null,
    modifier: Modifier
) {
    val updatedModifier = onClick?.let {
        modifier.clickable { onClick(video.sourceId) }
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
