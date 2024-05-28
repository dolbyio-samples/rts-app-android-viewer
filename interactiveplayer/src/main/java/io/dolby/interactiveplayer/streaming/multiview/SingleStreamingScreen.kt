package io.dolby.interactiveplayer.streaming.multiview

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.millicast.Media
import com.millicast.subscribers.remote.RemoteVideoTrack
import com.millicast.video.TextureViewRenderer
import io.dolby.interactiveplayer.R
import io.dolby.interactiveplayer.rts.ui.DolbyBackgroundBox
import io.dolby.interactiveplayer.rts.ui.LiveIndicator
import io.dolby.interactiveplayer.rts.ui.TopAppBar
import io.dolby.interactiveplayer.streaming.StatisticsView
import io.dolby.rtscomponentkit.data.multistream.prefs.MultiviewLayout
import io.dolby.rtscomponentkit.domain.StreamingData
import io.dolby.rtsviewer.uikit.button.StyledIconButton
import org.webrtc.RendererCommon
import org.webrtc.VideoSink

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SingleStreamingScreen(
    viewModel: MultiStreamingViewModel = hiltViewModel(),
    onBack: () -> Unit,
    onSettingsClick: (StreamingData?) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    val screenContentDescription = stringResource(id = R.string.streaming_screen_contentDescription)
    val selectedItem =
        uiState.videoTracks.firstOrNull { it.sourceId == uiState.selectedVideoTrackId }
    val mainSourceName = stringResource(id = io.dolby.rtscomponentkit.R.string.main_source_name)
    val (title, setTitle) = remember { mutableStateOf(selectedItem?.sourceId ?: mainSourceName) }
    val defaultLayout = viewModel.multiviewLayout.collectAsState()
    val showSourceLabels = viewModel.showSourceLabels.collectAsState()

    val streamingData = uiState.accountId?.let { accountId ->
        uiState.streamName?.let { streamName ->
            StreamingData(accountId, streamName)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = if (defaultLayout.value == MultiviewLayout.SingleStreamView) {
                    uiState.streamName ?: screenContentDescription
                } else "",
                onBack = {
                    onBack()
                    if (defaultLayout.value == MultiviewLayout.SingleStreamView) {
                        viewModel.disconnect()
                    }
                },
                onAction = { onSettingsClick(streamingData) }
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
            val initialPage =
                selectedItem?.let { uiState.videoTracks.indexOf(selectedItem) } ?: 0
            val pagerState = rememberPagerState(
                initialPage = initialPage,
                pageCount = { uiState.videoTracks.size }
            )
            val currentShownPage: MutableState<Boolean> = remember { mutableStateOf(true) }

            if (uiState.videoTracks.isNotEmpty()) {
                LaunchedEffect(pagerState) {
                    snapshotFlow { pagerState.currentPage }.collect { page ->
                        if (uiState.videoTracks.isNotEmpty()) {
                            viewModel.selectVideoTrack(uiState.videoTracks[page].sourceId)
                        }
                    }
                }
            }
            if (uiState.videoTracks.size > pagerState.currentPage) {
                setTitle(
                    uiState.videoTracks[pagerState.currentPage].sourceId ?: mainSourceName
                )
            }

            HorizontalPager(state = pagerState) { page ->
                VideoView(page, pagerState.currentPage, uiState, showSourceLabels.value)
            }
            PagerAudioTrackLifecycleObserver(uiState)

            LiveIndicator(
                modifier = Modifier.align(Alignment.TopStart),
                on = uiState.videoTracks.isNotEmpty() || uiState.audioTracks.isNotEmpty()
            )

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

            Statistics(viewModel, uiState, pagerState.currentPage, onBack)
        }
    }
}

@Composable
private fun Statistics(
    viewModel: MultiStreamingViewModel,
    uiState: MultiStreamingUiState,
    currentPage: Int,
    onBack: () -> Unit
) {
    val statisticsState by viewModel.statisticsState.collectAsStateWithLifecycle()

    BackHandler {
        if (statisticsState.showStatistics) {
            viewModel.updateStatistics(false)
        } else {
            onBack()
        }
    }
    if (statisticsState.showStatistics && statisticsState.statisticsData != null) {
        val statistics =
            viewModel.streamingStatistics(uiState.videoTracks[currentPage].currentMid)
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
    currentShownPage: Int,
    uiState: MultiStreamingUiState,
    displayLabels: Boolean
) {
    val context = LocalContext.current
    val videoRenderer = remember(context) {
        TextureViewRenderer(context).apply {
            init(Media.eglBaseContext, null)
        }
    }
    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier
                .aspectRatio(16F / 9)
                .align(Alignment.Center),
            factory = { videoRenderer },
            update = { view ->
                view.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT)
            }
        )
        PagerVideoTrackLifecycleObserver(
            video = uiState.videoTracks[page],
            videoSink = videoRenderer,
            page = page,
            currentShownPage = currentShownPage
        )
    }
}

@Composable
fun PagerAudioTrackLifecycleObserver(
    uiState: MultiStreamingUiState
) {
    val audioTrack =
        uiState.audioTracks.firstOrNull { it.sourceId == uiState.selectedAudioTrack }
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
fun PagerVideoTrackLifecycleObserver(
    video: RemoteVideoTrack,
    videoSink: VideoSink,
    page: Int,
    currentShownPage: Int
) {
    val lifecycleOwner = rememberUpdatedState(LocalLifecycleOwner.current)
    val pageIndex by rememberSaveable { mutableIntStateOf(page) }
    DisposableEffect(currentShownPage) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    video.disableAsync()
                }

                Lifecycle.Event.ON_RESUME -> {
                    if (pageIndex == currentShownPage) {
                        video.enableAsync(videoSink = videoSink)
                    }
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
