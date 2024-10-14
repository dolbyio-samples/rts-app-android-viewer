package io.dolby.rtsviewer.ui.streaming.container

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.dolby.rtscomponentkit.ui.DolbyBackgroundBox
import io.dolby.rtsviewer.R
import io.dolby.rtsviewer.ui.streaming.common.ErrorView
import io.dolby.rtsviewer.ui.streaming.stream.StreamScreen
import io.dolby.rtsviewer.utils.KeepScreenOn

@Composable
fun StreamingContainerScreen(viewModel: StreamingContainerViewModel = hiltViewModel(), onBack: () -> Unit) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val screenContentDescription = stringResource(id = R.string.streaming_screen_contentDescription)
    DolbyBackgroundBox(
        modifier = Modifier.semantics {
            contentDescription = screenContentDescription
        }
    ) {
        StreamingToolbarScreen(viewModel = viewModel)

        if (uiState.shouldStayOn) {
            KeepScreenOn(enabled = true)
        } else {
            KeepScreenOn(enabled = false)
        }

        uiState.streamError?.let {
            ErrorView(error = it)
        } ?: run {
            val count = uiState.streams.size.takeIf { it == 1 } ?: 2
            LazyVerticalGrid(
                columns = GridCells.Fixed(count),
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(top = 5.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                items(items = uiState.streams) { stream ->
                    StreamScreen(stream)
                }
            }
        }

        if (uiState.showSimulcastSettings) {
            SimulcastScreen(viewModel)
        } else if (uiState.showSettings) {
            SettingsScreen(viewModel)
        }

        BackHandler {
            if (uiState.showSimulcastSettings) {
                viewModel.onUiAction(StreamingContainerAction.UpdateSimulcastSettingsVisibility(false))
            } else if (uiState.showSettings) {
                viewModel.onUiAction(StreamingContainerAction.UpdateSettingsVisibility(false))
            } else {
                onBack.invoke()
            }
        }
    }
}
