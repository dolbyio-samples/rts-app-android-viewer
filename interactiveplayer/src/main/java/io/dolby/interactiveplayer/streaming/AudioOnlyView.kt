package io.dolby.interactiveplayer.streaming

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.dolby.interactiveplayer.R
import io.dolby.interactiveplayer.streaming.multiview.LiveIndicatorComponent
import io.dolby.rtsviewer.uikit.text.Text

@Composable
fun AudioOnlyView() {
    Box(modifier = Modifier.fillMaxSize()) {
        LiveIndicatorComponent(
            modifier = Modifier.align(Alignment.TopStart).padding(16.dp),
            on = true
        )

        Text(
            modifier = Modifier.align(Alignment.Center),
            text = stringResource(id = R.string.audio_only),
            style = MaterialTheme.typography.h3,
            color = MaterialTheme.colors.onPrimary,
            textAlign = TextAlign.Center
        )
    }
}

