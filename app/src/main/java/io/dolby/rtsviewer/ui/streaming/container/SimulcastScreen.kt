package io.dolby.rtsviewer.ui.streaming.container

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.dolby.rtsviewer.R
import io.dolby.rtsviewer.uikit.checkbox.CheckBoxComponent
import io.dolby.rtsviewer.uikit.text.Text
import io.dolby.rtsviewer.uikit.theme.DarkThemeColors

@Composable
fun SimulcastScreen(viewModel: StreamingContainerViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val focusRequester = remember { FocusRequester() }
    val screenContentDescription = stringResource(id = R.string.simulcastScreen_contentDescription)

    LaunchedEffect(uiState.simulcastSettingsEnabled) {
        try {
            focusRequester.requestFocus()
        } catch (e: IllegalStateException) {
            e.printStackTrace()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = .75f))
            .semantics { contentDescription = screenContentDescription }
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxHeight()
                .width(325.dp)
                .align(Alignment.TopEnd)
                .background(DarkThemeColors().neutralColor800)
                .padding(vertical = 42.dp)
                .padding(start = 25.dp, end = 22.dp)
        ) {
            item {
                Text(
                    text = stringResource(id = R.string.simulcast_title),
                    style = MaterialTheme.typography.h2,
                    color = MaterialTheme.colors.onBackground
                )
                Spacer(modifier = Modifier.height(26.dp))
            }

            items(
                items = uiState.availableStreamQualityItems,
                key = { it.hashCode() }
            ) { streamQualityType ->
                CheckBoxComponent(
                    text = stringResource(id = streamQualityType.titleResId),
                    checked = streamQualityType.titleResId == uiState.selectedStreamQualityTitleId,
                    onCheckedChange = {
                        viewModel.onUiAction(
                            StreamingContainerAction.UpdateSelectedStreamQuality(
                                streamQualityType
                            )
                        )
                    },
                    modifier = Modifier
                        .let {
                            if (streamQualityType.titleResId == uiState.selectedStreamQualityTitleId) {
                                return@let it.focusRequester(focusRequester)
                            }
                            return@let it
                        }
                )

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
