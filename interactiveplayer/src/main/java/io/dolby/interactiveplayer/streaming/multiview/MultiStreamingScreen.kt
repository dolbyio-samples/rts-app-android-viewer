package io.dolby.interactiveplayer.streaming.multiview

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import io.dolby.interactiveplayer.navigation.AppViewModel
import io.dolby.interactiveplayer.utils.KeepScreenOn
import io.dolby.rtscomponentkit.data.multistream.prefs.MultiviewLayout
import io.dolby.rtscomponentkit.domain.StreamingData

@Composable
fun MultiStreamingScreen(
    viewModel: MultiStreamingViewModel = hiltViewModel(),
    appViewModel: AppViewModel,
    onBack: () -> Unit,
    onMainClick: (String?) -> Unit,
    onSettingsClick: (StreamingData?) -> Unit
) {
    val multiviewLayout = viewModel.multiviewLayout.collectAsState()
    val uiState = viewModel.uiState.collectAsState()
    val streamingData = uiState.value.accountId?.let { accountId ->
        uiState.value.streamName?.let { streamName ->
            StreamingData(accountId, streamName)
        }
    }
    LaunchedEffect(Unit) {
        appViewModel.enablePip(true)
    }
    KeepScreenOn(true)
    BackHandler(true) {
        viewModel.disconnect()
        onBack()
    }
    val audioTrack =
        uiState.value.audioTracks.firstOrNull { it.sourceId == uiState.value.selectedAudioTrack }
    AudioTrackLifecycleObserver(audioTrack)
    val lazyVerticalGridState = rememberLazyGridState()
    when (multiviewLayout.value) {
        MultiviewLayout.GridView -> {
            GridViewScreen(
                viewModel = viewModel,
                onBack = onBack,
                onMainClick = {
                    viewModel.selectVideoTrack(it)
                    onMainClick(it)
                },
                onSettingsClick = { onSettingsClick(streamingData) }
            )
        }

        MultiviewLayout.SingleStreamView -> {
            SingleStreamingScreen(
                viewModel = viewModel,
                appViewModel = appViewModel,
                onBack = onBack,
                onSettingsClick = { onSettingsClick(streamingData) }
            )
        }

        else -> {
            ListViewScreen(
                viewModel = viewModel,
                onBack = onBack,
                onMainClick = onMainClick,
                onSettingsClick = { onSettingsClick(streamingData) }
            )
        }
    }
}
