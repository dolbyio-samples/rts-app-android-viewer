package io.dolby.rtsviewer.ui.streaming.stream

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.millicast.devices.track.TrackType
import io.dolby.rtsviewer.R
import io.dolby.rtsviewer.uikit.text.Text
import io.dolby.rtsviewer.uikit.theme.getColorPalette
import io.dolby.rtsviewer.utils.formattedByteCount

// move to its own package?
@Composable
fun StatisticsScreen(viewModel: StreamViewModel, modifier: Modifier = Modifier) {
    val statistics = viewModel.subscriberStats.collectAsState(initial = null)
    val statisticsTitle = stringResource(id = R.string.streaming_statistics_title)

    Box(
        modifier = modifier
            .size(width = 420.dp, height = 360.dp)
            .background(
                color = getColorPalette().neutralColor800,
                shape = MaterialTheme.shapes.large
            )
            .clip(MaterialTheme.shapes.large)
            .semantics { contentDescription = statisticsTitle }
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .verticalScroll(rememberScrollState())
                .padding(vertical = 8.dp)
                .padding(start = 16.dp, end = 16.dp)
        ) {
            Text(
                text = stringResource(id = R.string.streaming_statistics_title),
                style = MaterialTheme.typography.body2,
                color = MaterialTheme.colors.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row {
                Text(
                    text = stringResource(id = R.string.statisticsScreen_name),
                    style = MaterialTheme.typography.body2,
                    color = MaterialTheme.colors.onBackground,
                    textAlign = TextAlign.Left,
                    modifier = Modifier.width(155.dp)
                )
                Text(
                    text = stringResource(id = R.string.statisticsScreen_value),
                    style = MaterialTheme.typography.body2,
                    color = MaterialTheme.colors.onBackground,
                    textAlign = TextAlign.Left
                )
            }

            statistics.value?.let {
                StatsRow(title = "Stream View Id", value = it.streamViewId)
                StatsRow(title = "Subscriber Id", value = it.subscriberId)
                StatsRow(title = "Cluster Id", value = it.clusterId)

                it.trackStats().forEach { trackStats ->
                    when (trackStats.type) {
                        TrackType.Video -> {
                            StatsRow(title = "MID", value = trackStats.mid)
                            StatsRow(
                                title = "Decoder Implementation",
                                value = trackStats.decoderImplementation ?: ""
                            )
                            StatsRow(
                                title = "Processing Delay",
                                value = trackStats.processingDelay?.toString() ?: ""
                            )
                            StatsRow(
                                title = "Decode time",
                                value = trackStats.decodeTime?.toString() ?: ""
                            )
                            StatsRow(
                                title = "Video Resolution",
                                value = (trackStats.frameWidth?.toString()
                                    ?: "") + "x" + (trackStats.frameHeight?.toString() ?: "")
                            )
                            StatsRow(
                                title = "FPS",
                                value = formattedByteCount(
                                    trackStats.framesPerSecond?.toLong() ?: 0
                                )
                            )
                            StatsRow(
                                title = "Video Total Received",
                                value = trackStats.bytesReceived.toString()
                            )
                        }

                        TrackType.Audio -> {
                            StatsRow(
                                title = "Audio Total Received",
                                value = formattedByteCount(
                                    trackStats.framesPerSecond?.toLong() ?: 0
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatsRow(title: String, value: String, modifier: Modifier = Modifier) {
    Row(modifier = modifier) {
        Text(
            text = title,
            style = MaterialTheme.typography.overline,
            color = getColorPalette().grayLight,
            textAlign = TextAlign.Left,
            modifier = Modifier
                .width(160.dp)
                .align(Alignment.CenterVertically)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.overline,
            color = MaterialTheme.colors.onBackground,
            textAlign = TextAlign.Left,
            modifier = Modifier
                .align(Alignment.CenterVertically)
        )
    }
}
