package io.dolby.rtsviewer.ui.streaming

import android.content.Context
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.dolby.rtsviewer.R
import io.dolby.rtsviewer.uikit.button.StyledIconButton
import io.dolby.rtsviewer.uikit.theme.getColorPalette

private const val TAG = "StatisticsView"

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun StatisticsView(viewModel: StreamingViewModel, modifier: Modifier = Modifier) {
    val statistics by viewModel.streamingStatistics.collectAsStateWithLifecycle(initialValue = null)
    val statisticsTitle = stringResource(id = R.string.streaming_statistics_title)
    val localClipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    Box(
        modifier = modifier
            .size(width = 480.dp, height = 720.dp)
            .background(
                color = getColorPalette().neutralColor800,
                shape = MaterialTheme.shapes.large
            )
            .padding(0.dp)
            .semantics { contentDescription = statisticsTitle },
        contentAlignment = Alignment.TopEnd
    ) {
        Row {
            Text(
                modifier = modifier.padding(5.dp),
                text = stringResource(id = R.string.streaming_statistics_title),
                style = MaterialTheme.typography.h4,
                color = MaterialTheme.colors.onBackground
            )
            StyledIconButton(
                icon = painterResource(id = io.dolby.uikit.R.drawable.ic_close),
                onClick = {
                    viewModel.updateStatistics(false)
                }
            )
        }

        Box(
            modifier = modifier
                .size(width = 480.dp, height = 600.dp)
                .background(
                    color = getColorPalette().neutralColor800,
                    shape = MaterialTheme.shapes.large
                )
                .padding(0.dp)
                .semantics { contentDescription = statisticsTitle }
                .combinedClickable(
                    onLongClick = {
                        // https://developer.android.com/develop/ui/views/touch-and-input/copy-paste
                        localClipboardManager.setText(AnnotatedString(formattedText(context, statistics)))
                    },
                    onClick = {},
                ),
            contentAlignment = Alignment.TopEnd
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 18.dp)
                    .padding(start = 8.dp, end = 8.dp)
            ) {
                Spacer(modifier = Modifier.height(10.dp))
                Row {
                    Text(
                        text = stringResource(id = R.string.statisticsScreen_name),
                        style = MaterialTheme.typography.h5,
                        color = MaterialTheme.colors.onBackground,
                        textAlign = TextAlign.Left,
                        modifier = Modifier.width(155.dp)
                    )
                    Spacer(modifier = Modifier.width(15.dp))
                    Text(
                        text = stringResource(id = R.string.statisticsScreen_value),
                        style = MaterialTheme.typography.h5,
                        color = MaterialTheme.colors.onBackground,
                        textAlign = TextAlign.Left
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))

                statistics?.forEach {
                    StatisticsRow(
                        title = stringResource(id = it.first),
                        value = it.second
                    )
                    Spacer(modifier = Modifier.height(5.dp))
                }
            }

            if (statistics.isNullOrEmpty()) {
                Text(
                    text = stringResource(id = R.string.statisticsScreen_no_data),
                    style = MaterialTheme.typography.body2,
                    color = getColorPalette().grayLight,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}

fun formattedText(context: Context, statistics: List<Pair<Int, String>>?) : String {
    val textBuilder = StringBuilder()
    statistics?.forEach  {(key, value) ->
        val str = context.resources.getString(key)
        textBuilder.append("$str: $value\n")
    }
    return textBuilder.toString()
}
@Composable
fun StatisticsRow(title: String, value: String, modifier: Modifier = Modifier) {
    Row(modifier = modifier) {
        Text(
            text = title,
            style = MaterialTheme.typography.body1,
            color = getColorPalette().grayLight,
            textAlign = TextAlign.Left,
            modifier = Modifier
                .width(160.dp)
                .align(Alignment.CenterVertically)
        )
        Spacer(modifier = Modifier.width(15.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.body1,
            color = MaterialTheme.colors.onBackground,
            textAlign = TextAlign.Left,
            modifier = Modifier
                .align(Alignment.CenterVertically)
        )
    }
}
