package io.dolby.rtsviewer.ui.streaming

import androidx.compose.runtime.Composable
import androidx.tv.material3.Text
import io.dolby.rtsviewer.data.StreamingData

@Composable
fun StreamingScreen(streamingData: StreamingData) {
    Text("${streamingData.streamName} / ${streamingData.accountId}")
}