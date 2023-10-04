package io.dolby.interactiveplayer.streaming.multiview

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.hilt.navigation.compose.hiltViewModel
import io.dolby.interactiveplayer.preferenceStore.MultiviewLayout

@Composable
fun MultiStreamingScreen(
    viewModel: MultiStreamingViewModel = hiltViewModel(),
    onBack: () -> Unit,
    onMainClick: (String?) -> Unit
) {
    val multiviewLayout = viewModel.multiviewLayout.collectAsState()
    when (multiviewLayout.value) {
        MultiviewLayout.GridView -> {
            GridViewScreen(onBack = onBack, onMainClick = onMainClick)
        }
        MultiviewLayout.SingleStreamView -> {
            SingleStreamingScreen(viewModel = viewModel, onBack = onBack)
        }
        else -> {
            ListViewScreen(viewModel = viewModel, onBack = onBack, onMainClick = onMainClick)
        }
    }
}
