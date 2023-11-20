package io.dolby.interactiveplayer.streaming.multiview

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.hilt.navigation.compose.hiltViewModel
import io.dolby.interactiveplayer.preferenceStore.MultiviewLayout
import io.dolby.interactiveplayer.rts.domain.StreamingData

@Composable
fun MultiStreamingScreen(
    viewModel: MultiStreamingViewModel = hiltViewModel(),
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
    BackHandler(true) {
        viewModel.disconnect()
        onBack()
    }
    when (multiviewLayout.value) {
        MultiviewLayout.GridView -> {
            GridViewScreen(
                viewModel = viewModel,
                onBack = onBack,
                onMainClick = onMainClick,
                onSettingsClick = { onSettingsClick(streamingData) }
            )
        }

        MultiviewLayout.SingleStreamView -> {
            SingleStreamingScreen(
                viewModel = viewModel,
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
