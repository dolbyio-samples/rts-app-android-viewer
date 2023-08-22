package io.dolby.rtsviewer.ui.streaming.multiview

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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

    Scaffold(
        topBar = {
            TopAppBar(title = uiState.streamName.toString()) {
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
                    Log.d(
                        "===>",
                        "video size: ${uiState.videoTracks.size}, audio size: ${uiState.audioTracks.size}"
                    )
                    Box(
                        modifier = Modifier.align(Alignment.Center)
                    ) {
                        SetupVolumeControlAudioStream()
                        Column {
                            Box(modifier = Modifier.clickable {
                                onMainClick(uiState.videoTracks.firstOrNull()?.id)
                            }) {
                                val selectedVideoTrackId = uiState.selectedVideoTrackId
                                AndroidView(
                                    factory = { context ->
                                        val view = VideoRenderer(context)
                                        view.setZOrderOnTop(true)
                                        view.setZOrderMediaOverlay(true)
                                        view
                                    },
                                    update = { view ->
                                        view.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT)
                                        uiState.videoTracks.firstOrNull()?.videoTrack?.setRenderer(
                                            view
                                        )
                                    }
                                )
                                Text(
                                    text = selectedVideoTrackId ?: "Main",
                                    modifier = Modifier.align(
                                        Alignment.BottomStart
                                    )
                                )
                            }
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(count = 2),
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .padding(top = 5.dp),
                                verticalArrangement = Arrangement.spacedBy(5.dp),
                                horizontalArrangement = Arrangement.spacedBy(5.dp),
                            ) {
                                items(items = uiState.videoTracks.filter { it.sourceId != uiState.selectedVideoTrackId }) { videoTrack ->
                                    Box {
                                        AndroidView(
                                            modifier = Modifier
                                                .aspectRatio(1F)
                                                .clickable {
                                                    viewModel.selectVideoTrack(videoTrack.sourceId)
                                                },
                                            factory = { context -> VideoRenderer(context) },
                                            update = { view ->
                                                view.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT)
                                                videoTrack.videoTrack.setRenderer(view)
                                            }
                                        )
                                        Text(
                                            text = videoTrack.sourceId ?: "Main",
                                            modifier = Modifier.align(
                                                Alignment.BottomStart
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
