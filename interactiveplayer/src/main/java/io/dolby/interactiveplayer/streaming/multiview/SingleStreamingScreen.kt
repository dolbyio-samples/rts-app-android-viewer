package io.dolby.interactiveplayer.streaming.multiview

import android.app.PictureInPictureParams
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
import androidx.compose.runtime.setValue
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
import io.dolby.interactiveplayer.utils.KeepScreenOn
import io.dolby.interactiveplayer.utils.findActivity
import io.dolby.interactiveplayer.utils.rememberIsInPipMode
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
    val inPipMode = rememberIsInPipMode()
    var currentShouldEnterPipMode by remember { mutableStateOf(true) }
    val currentSourceId = remember { mutableStateOf(selectedItem?.sourceId) }
    val streamingData = uiState.accountId?.let { accountId ->
        uiState.streamName?.let { streamName ->
            StreamingData(accountId, streamName)
        }
    }

    KeepScreenOn(enabled = uiState.error == null && uiState.videoTracks.isNotEmpty())

    Scaffold(
        topBar = {
            if (!inPipMode) {
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

            if (uiState.videoTracks.isNotEmpty()) {
                LaunchedEffect(Unit) {
                    pagerState.scrollToPage(initialPage)
                }
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
                VideoView(
                    page,
                    pagerState.currentPage,
                    uiState,
                    showSourceLabels.value,
                    currentSourceId
                )
            }
            PagerAudioTrackLifecycleObserver(uiState, currentSourceId)

            LiveIndicator(
                modifier = Modifier.align(Alignment.TopStart),
                on = uiState.videoTracks.isNotEmpty() || uiState.audioTracks.isNotEmpty()
            )

            QualitySelector(viewModel = viewModel)
            if (!inPipMode) {
                StyledIconButton(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(20.dp),
                    icon = painterResource(id = io.dolby.uikit.R.drawable.icon_info),
                    onClick = {
                        viewModel.updateStatistics(true)
                    }
                )
            }

            Statistics(viewModel, uiState, pagerState.currentPage, onBack = {
                currentShouldEnterPipMode = false
                onBack()
            })
        }
    }
    val context = LocalContext.current
    DisposableEffect(context) {
        val onUserLeaveBehavior: () -> Unit = {
            if (currentShouldEnterPipMode) {
                context.findActivity()
                    .enterPictureInPictureMode(PictureInPictureParams.Builder().build())
            }
        }
        context.findActivity().addOnUserLeaveHintListener(
            onUserLeaveBehavior
        )
        onDispose {
            context.findActivity().removeOnUserLeaveHintListener(
                onUserLeaveBehavior
            )
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
    val inPipMode = rememberIsInPipMode()
    BackHandler {
        if (statisticsState.showStatistics) {
            viewModel.updateStatistics(false)
        } else {
            onBack()
        }
    }
    if (statisticsState.showStatistics && statisticsState.statisticsData != null && !inPipMode) {
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
    displayLabels: Boolean,
    currentSourceId: MutableState<String?>
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
            currentShownPage = currentShownPage,
            currentSourceId
        )
    }
}

@Composable
fun PagerAudioTrackLifecycleObserver(
    uiState: MultiStreamingUiState,
    currentSourceId: MutableState<String?>
) {
    val inPipMode = rememberIsInPipMode()
    val audioTrack =
        uiState.audioTracks.firstOrNull { it.sourceId == uiState.selectedAudioTrack }
    audioTrack?.let { audio ->
        val lifecycleOwner = rememberUpdatedState(LocalLifecycleOwner.current)
        LaunchedEffect(inPipMode) {
            if (inPipMode) {
                audio.enableAsync()
            } else {
                audio.disableAsync()
            }
        }
        DisposableEffect(audioTrack) {
            val observer = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_PAUSE -> {
                        audio.disableAsync()
                    }

                    Lifecycle.Event.ON_RESUME -> {
                        currentSourceId.value = audio.sourceId
                        audio.enableAsync()
                    }

                    else -> {
                    }
                }
            }
            val lifecycle = lifecycleOwner.value.lifecycle
            lifecycle.addObserver(observer)
            onDispose {
                audio.disableAsync()
                lifecycle.removeObserver(observer)
            }
        }
    }
}

@Composable
fun PagerVideoTrackLifecycleObserver(
    video: RemoteVideoTrack,
    videoSink: VideoSink,
    page: Int,
    currentShownPage: Int,
    currentSourceId: MutableState<String?>
) {
    val lifecycleOwner = rememberUpdatedState(LocalLifecycleOwner.current)
    val inPipMode = rememberIsInPipMode()
    val pageIndex by rememberSaveable { mutableIntStateOf(page) }
    LaunchedEffect(inPipMode) {
        if (inPipMode) {
            if (currentSourceId.value == video.sourceId) {
                video.enableAsync(videoSink = videoSink)
            }
        } else {
            video.disableAsync()
        }
    }
    DisposableEffect(currentShownPage) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    video.disableAsync()
                }

                Lifecycle.Event.ON_RESUME -> {
                    if (pageIndex == currentShownPage) {
                        video.enableAsync(videoSink = videoSink)
                        currentSourceId.value = video.sourceId
                    }
                }

                else -> {
                }
            }
        }
        val lifecycle = lifecycleOwner.value.lifecycle
        lifecycle.addObserver(observer)
        onDispose {
            video.disableAsync()
            lifecycle.removeObserver(observer)
        }
    }
}
