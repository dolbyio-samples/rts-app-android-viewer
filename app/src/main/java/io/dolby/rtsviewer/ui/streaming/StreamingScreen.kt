package io.dolby.rtsviewer.ui.streaming

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.paint
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.viewinterop.AndroidView
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.millicast.VideoRenderer
import io.dolby.rtscomponentkit.R
import org.webrtc.RendererCommon

@Composable
fun StreamingScreen(viewModel: StreamingViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    Box(
        modifier = Modifier
            .fillMaxSize()
            .paint(
                painter = painterResource(id = R.drawable.background),
                contentScale = ContentScale.FillBounds
            )
    ) {
        ConstraintLayout(
            modifier = Modifier
                .fillMaxSize()
                .align(Alignment.Center)
        ) {
            val (text, progress, videoView, error) = createRefs()
            if (uiState.connecting && uiState.error == null) {
                CircularProgressIndicator(
                    modifier = Modifier.constrainAs(progress) {
                        centerVerticallyTo(parent)
                        centerHorizontallyTo(parent)
                    }
                )
            } else if (uiState.error != null) {
                Text(
                    text = "${uiState.error}",
                    style = MaterialTheme.typography.h3,
                    color = MaterialTheme.colors.onPrimary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.constrainAs(error) {
                        centerHorizontallyTo(parent)
                        centerVerticallyTo(parent)
                    }
                )
            } else {
                AndroidView(
                    factory = { context -> VideoRenderer(context) },
                    update = { view ->
                        view.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT)
                        uiState.videoTrack?.setRenderer(view)
                    },
                    modifier = Modifier.constrainAs(videoView) {
                        centerHorizontallyTo(parent)
                        centerVerticallyTo(parent)
                    }
                )
            }
            Text(
                text = uiState.streamName,
                color = MaterialTheme.colors.onPrimary,
                modifier = Modifier.constrainAs(text) {
                    top.linkTo(parent.top)
                }
            )
        }
    }
}
