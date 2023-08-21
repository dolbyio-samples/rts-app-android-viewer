package io.dolby.rtsviewer.ui.streaming

import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.millicast.VideoRenderer
import io.dolby.rtscomponentkit.ui.DolbyBackgroundBox
import io.dolby.rtscomponentkit.ui.TopAppBar
import io.dolby.rtsviewer.R
import io.dolby.rtsviewer.ui.streaming.multiview.MultiStreamingViewModel
import org.webrtc.RendererCommon

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SingleStreamingScreen(
    viewModel: MultiStreamingViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val screenContentDescription = stringResource(id = R.string.streaming_screen_contentDescription)

    Scaffold(
        topBar = {
            TopAppBar(title = "Stream Details") {
                onBack()
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
//            if (uiState.videoTracks.isNotEmpty()) {
//                AndroidView(
//                    factory = { context -> VideoRenderer(context) },
//                    update = { view ->
//                        view.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT)
//                        uiState.videoTracks[0].videoTrack.setRenderer(view)
//                    }
//                )
//            }
            val pagerState = rememberPagerState(initialPage = 2, pageCount = { uiState.videoTracks.size })
//            pagerState.scrollToPage(uiState.videoTracks.indexOf(uiState.videoTracks.first { it.id == uiState.selectedVideoTrackId }))
//            Log.d("===>", "Single streaming screen pages: ${uiState.videoTracks.size}, currentPage = ${pagerState.currentPage}")
//
            HorizontalPager(state = pagerState) { page ->
                Column {
                    Text("page ${page}")
                    Text("currentPage ${pagerState.currentPage}")
                    AndroidView(
                        modifier = Modifier.aspectRatio(1F),
                        factory = { context ->
                            val view = VideoRenderer(context)
                            Log.e("+++++++++>", "View $page created: $view")
                            view
                        },
                        update = { view ->
                            view.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT)
                            uiState.videoTracks[page].videoTrack.setRenderer(view)
                            Log.e("+++++++++>", "View $page updated: $view")
                        }
                    )
                }
            }
        }
    }
}
