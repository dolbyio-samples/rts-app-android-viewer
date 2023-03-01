package io.dolby.rtsviewer.ui.streaming

import androidx.compose.runtime.Composable
import androidx.tv.material3.Text
import io.dolby.rtscomponentkit.domain.StreamingData

@Composable
fun StreamingScreen(viewModel: StreamingViewModel, streamingData: StreamingData) {
    Text("${streamingData.streamName} / ${streamingData.accountId}")
}