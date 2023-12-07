package io.dolby.rtsviewer.ui.streaming

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.dolby.rtsviewer.R
import io.dolby.rtsviewer.uikit.text.Text

@Composable
fun ErrorView(error: Error) {
    fun Error.titleResourceId(): Int {
        return when (this) {
            Error.NO_INTERNET_CONNECTION -> R.string.stream_network_disconnected_label
            Error.STREAM_NOT_ACTIVE -> R.string.stream_offline_title_label
        }
    }

    fun Error.subtitleResourceId(): Int? {
        return when (this) {
            Error.NO_INTERNET_CONNECTION -> null
            Error.STREAM_NOT_ACTIVE -> R.string.stream_offline_subtitle_label
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.background.copy(alpha = 0.75F))
    ) {
        Text(
            text = stringResource(id = error.titleResourceId()),
            style = MaterialTheme.typography.h2,
            color = MaterialTheme.colors.onPrimary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        error.subtitleResourceId()?.let {
            Text(
                text = stringResource(id = it),
                style = MaterialTheme.typography.body1,
                color = MaterialTheme.colors.onPrimary,
                textAlign = TextAlign.Center
            )
        }
    }
}
