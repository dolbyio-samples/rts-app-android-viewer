package io.dolby.rtsviewer.ui.streaming.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.dolby.rtsviewer.R
import io.dolby.rtsviewer.uikit.text.Text
import java.util.Locale

@Composable
fun LiveIndicatorView(modifier: Modifier = Modifier, isLive: Boolean) {
    val liveIndicatorTitle = stringResource(id = R.string.streaming_statistics_title)
    Box(
        modifier = modifier
            .semantics { contentDescription = liveIndicatorTitle }
    ) {
        val liveIndicatorLabel =
            if (isLive) stringResource(R.string.live_label)
            else stringResource(R.string.offline_label)
        val liveIndicatorBackgroundColor =
            if (isLive) MaterialTheme.colors.error else MaterialTheme.colors.surface
        val liveIndicatorContentDescription =
            "$liveIndicatorLabel ${stringResource(id = R.string.liveIndicator_contentDescription)}"
        Text(
            text = liveIndicatorLabel.uppercase(Locale.ROOT),
            style = MaterialTheme.typography.caption,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colors.onPrimary,
            modifier = Modifier
                .background(liveIndicatorBackgroundColor, shape = RoundedCornerShape(2.dp))
                .padding(horizontal = 10.dp, vertical = 3.dp)
                .semantics { contentDescription = liveIndicatorContentDescription }
        )
    }
}