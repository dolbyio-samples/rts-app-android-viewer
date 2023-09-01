package io.dolby.rtsviewer.ui.streaming

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
import io.dolby.rtscomponentkit.ui.DolbyBackgroundBox
import io.dolby.rtscomponentkit.ui.LiveIndicator
import io.dolby.rtscomponentkit.ui.TopAppBar
import io.dolby.rtsviewer.R
import io.dolby.rtsviewer.ui.streaming.multiview.MultiStreamingViewModel
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
                pageCount = { uiState.videoTracks.size })
            setTitle(uiState.videoTracks[pagerState.currentPage].sourceId ?: mainSourceName)

            HorizontalPager(state = pagerState) { page ->
                Box(modifier = Modifier.fillMaxSize()) {
                    AndroidView(
                        modifier = Modifier
                            .aspectRatio(16F / 9)
                            .align(Alignment.Center),
                        factory = { context -> VideoRenderer(context) },
                        update = { view ->
                            view.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT)
                            view.release()
                            uiState.videoTracks[page].videoTrack.setRenderer(view)
                        }
                    )
                }
            }

            LiveIndicator(
                modifier = Modifier.align(Alignment.TopStart),
                on = uiState.videoTracks.isNotEmpty() || uiState.audioTracks.isNotEmpty()
            )

            StyledIconButton(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(20.dp),
                icon = painterResource(id = io.dolby.uikit.R.drawable.icon_info),
                onClick = {
                    viewModel.updateStatistics(true)
                }
            )

            if (uiState.showStatistics && uiState.statisticsData != null) {
                val statistics =
                    viewModel.streamingStatistics(uiState.videoTracks[pagerState.currentPage].id)
                StatisticsView(
                    statistics = statistics,
                    updateStatistics = { showStatistics: Boolean ->
                        viewModel.updateStatistics(showStatistics)
                    },
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(horizontal = 22.dp, vertical = 15.dp)
                )
            }
        }
    }
}
