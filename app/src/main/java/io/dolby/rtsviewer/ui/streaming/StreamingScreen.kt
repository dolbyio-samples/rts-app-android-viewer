package io.dolby.rtsviewer.ui.streaming

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.paint
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.millicast.VideoRenderer
import io.dolby.rtsviewer.MainActivity
import io.dolby.rtsviewer.R
import io.dolby.rtsviewer.utils.KeepScreenOn
import io.dolby.rtsviewer.utils.SetupVolumeControlAudioStream
import io.dolby.rtsviewer.utils.findActivity
import org.webrtc.RendererCommon
import java.util.Locale

@Composable
fun StreamingScreen(viewModel: StreamingViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val screenContentDescription = stringResource(id = R.string.streaming_screen_contentDescription)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .paint(
                painter = painterResource(id = io.dolby.rtscomponentkit.R.drawable.background),
                contentScale = ContentScale.FillBounds
            ),
        contentAlignment = Alignment.Center
    ) {
        ConstraintLayout(
            modifier = Modifier
                .fillMaxSize()
                .align(Alignment.Center)
                .semantics { contentDescription = screenContentDescription }
        ) {
            val context = LocalContext.current
            val (liveIndicator, progress, videoView, error) = createRefs()
            when {
                uiState.connecting && uiState.error == null -> {
                    CircularProgressIndicator(
                        modifier = Modifier.constrainAs(progress) {
                            centerVerticallyTo(parent)
                            centerHorizontallyTo(parent)
                        }
                    )
                }
                uiState.error != null -> {
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
                }
                uiState.subscribed -> {
                    Box(
                        modifier = Modifier.constrainAs(videoView) {
                            centerHorizontallyTo(parent)
                            centerVerticallyTo(parent)
                        }
                    ) {
                        AndroidView(
                            factory = { context -> VideoRenderer(context) },
                            update = { view ->
                                view.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT)
                                uiState.videoTrack?.setRenderer(view)
                            }
                        )
                        SetupVolumeControlAudioStream()
                    }
                }
                uiState.disconnected -> {
                    (context.findActivity() as? MainActivity?)?.unregisterVolumeObserverIfExists()
                }
            }

            if (uiState.audioTrack != null) {
                val audioTrack = uiState.audioTrack
                (context.findActivity() as? MainActivity?)?.addVolumeObserver(audioTrack)
            }

            if (uiState.subscribed) {
                KeepScreenOn(enabled = true)
            } else {
                KeepScreenOn(enabled = false)
            }

            val liveIndicatorLabel =
                if (uiState.subscribed) stringResource(R.string.live_label)
                else stringResource(R.string.offline_label)
            val liveIndicatorBackgroundColor =
                if (uiState.subscribed) MaterialTheme.colors.error else MaterialTheme.colors.surface
            Text(
                text = liveIndicatorLabel.uppercase(Locale.ROOT),
                style = MaterialTheme.typography.caption,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colors.onPrimary,
                modifier = Modifier
                    .constrainAs(liveIndicator) {
                        top.linkTo(parent.top, margin = 14.dp)
                        start.linkTo(parent.start, margin = 20.dp)
                    }
                    .background(liveIndicatorBackgroundColor, shape = RoundedCornerShape(2.dp))
                    .padding(horizontal = 10.dp, vertical = 3.dp)
            )
        }
    }
}
