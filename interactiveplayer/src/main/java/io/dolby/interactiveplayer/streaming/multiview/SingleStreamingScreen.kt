package io.dolby.interactiveplayer.streaming.multiview

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.millicast.VideoRenderer
import io.dolby.interactiveplayer.R
import io.dolby.interactiveplayer.rts.data.MultiStreamingRepository
import io.dolby.interactiveplayer.rts.ui.DolbyBackgroundBox
import io.dolby.interactiveplayer.rts.ui.LiveIndicator
import io.dolby.interactiveplayer.rts.ui.TopAppBar
import io.dolby.interactiveplayer.streaming.StatisticsView
import io.dolby.rtsviewer.uikit.button.StyledIconButton
import org.webrtc.RendererCommon

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SingleStreamingScreen(
    viewModel: MultiStreamingViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val screenContentDescription = stringResource(id = R.string.streaming_screen_contentDescription)
    val selectedItem =
        uiState.videoTracks.firstOrNull { it.sourceId == uiState.selectedVideoTrackId }
    val mainSourceName = stringResource(id = R.string.main_source_name)
    val (title, setTitle) = remember { mutableStateOf(selectedItem?.sourceId ?: mainSourceName) }

    Scaffold(
        topBar = { TopAppBar(title = title) { onBack() } }
    ) { paddingValues ->
        DolbyBackgroundBox(
            modifier = Modifier
                .semantics {
                    contentDescription = screenContentDescription
                }
                .padding(paddingValues)
        ) {
            val initialPage = selectedItem?.let { uiState.videoTracks.indexOf(selectedItem) } ?: 0
            val pagerState = rememberPagerState(
                initialPage = initialPage,
                pageCount = { uiState.videoTracks.size }
            )
            setTitle(uiState.videoTracks[pagerState.currentPage].sourceId ?: mainSourceName)

            HorizontalPager(state = pagerState) { page ->
                VideoView(page, uiState, viewModel)
            }

            LiveIndicator(
                modifier = Modifier.align(Alignment.TopStart),
                on = uiState.videoTracks.isNotEmpty() || uiState.audioTracks.isNotEmpty()
            )

//            QualityLabel(
//                viewModel = viewModel,
//                video = uiState.videoTracks[pagerState.currentPage],
//                modifier = Modifier.align(Alignment.BottomEnd)
//            )

            QualitySelector(viewModel = viewModel)

            StyledIconButton(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(20.dp),
                icon = painterResource(id = io.dolby.uikit.R.drawable.icon_info),
                onClick = {
                    viewModel.updateStatistics(true)
                }
            )

            Statistics(viewModel, uiState, pagerState.currentPage)
        }
    }
}

@Composable
private fun Statistics(
    viewModel: MultiStreamingViewModel,
    uiState: MultiStreamingUiState,
    currentPage: Int
) {
    val statisticsState by viewModel.statisticsState.collectAsStateWithLifecycle()
    if (statisticsState.showStatistics && statisticsState.statisticsData != null) {
        val statistics =
            viewModel.streamingStatistics(uiState.videoTracks[currentPage].id)
        Box(modifier = Modifier.fillMaxSize()) {
            StatisticsView(
                statistics = statistics,
                updateStatistics = { showStatistics: Boolean ->
                    viewModel.updateStatistics(showStatistics)
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 22.dp, vertical = 15.dp)
            )
        }
    }
}

@Composable
private fun VideoView(
    page: Int,
    uiState: MultiStreamingUiState,
    viewModel: MultiStreamingViewModel
) {
    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier
                .aspectRatio(16F / 9)
                .align(Alignment.Center),
            factory = { context ->
                val view = VideoRenderer(context)
                view
            },
            update = { view ->
                view.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT)
                uiState.videoTracks[page].videoTrack.setRenderer(view)
                val isSelected =
                    uiState.selectedVideoTrackId == uiState.videoTracks[page].sourceId
                viewModel.playVideo(
                    uiState.videoTracks[page],
                    if (isSelected) MultiStreamingRepository.VideoQuality.AUTO else MultiStreamingRepository.VideoQuality.LOW
                )
            },
            onRelease = { view ->
                viewModel.stopVideo(uiState.videoTracks[page])
                view.release()
            }
        )
    }
}
