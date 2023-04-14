package io.dolby.rtsviewer.ui.streaming

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.dolby.rtsviewer.R
import io.dolby.rtsviewer.uikit.button.StateButton
import io.dolby.rtsviewer.uikit.switch.SwitchComponent
import io.dolby.rtsviewer.uikit.theme.DarkThemeColors
import io.dolby.rtsviewer.utils.titleResourceId

@Composable
fun SettingsScreen(viewModel: StreamingViewModel) {
    val showLiveIndicator = viewModel.showLiveIndicator.collectAsStateWithLifecycle()
    val showStatistics = viewModel.showStatistics.collectAsStateWithLifecycle()

    val focusRequester = remember { FocusRequester() }
    val screenContentDescription = stringResource(id = R.string.settingsScreen_contentDescription)

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = .75f))
            .focusRequester(focusRequester)
            .semantics { contentDescription = screenContentDescription }
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .width(325.dp)
                .align(Alignment.TopEnd)
                .background(DarkThemeColors().neutralColor800)
                .verticalScroll(rememberScrollState())
                .padding(vertical = 42.dp)
                .padding(start = 25.dp, end = 22.dp)
        ) {
            Text(
                text = stringResource(id = R.string.settings_title),
                style = MaterialTheme.typography.h2,
                color = MaterialTheme.colors.onBackground
            )
            Spacer(modifier = Modifier.height(26.dp))
            StateButton(
                text = stringResource(id = R.string.simulcast_title),
                startIcon = painterResource(id = io.dolby.uikit.R.drawable.icon_simulcast),
                endIcon = painterResource(id = io.dolby.uikit.R.drawable.ic_arrow_right),
                stateText = stringResource(id = uiState.selectedStreamQualityType.titleResourceId()),
                isEnabled = uiState.streamQualityTypes.isNotEmpty(),
                onClick = { viewModel.updateShowSimulcastSettings(true) }
            )
            Spacer(modifier = Modifier.height(12.dp))
            SwitchComponent(
                text = stringResource(id = R.string.streaming_statistics_title),
                startIcon = painterResource(id = io.dolby.uikit.R.drawable.icon_info),
                checked = showStatistics.value,
                isEnabled = true,
                onCheckedChange = { viewModel.updateStatistics(state = it) }
            )
            Spacer(modifier = Modifier.height(12.dp))
            SwitchComponent(
                text = stringResource(id = R.string.live_indicator_title),
                startIcon = painterResource(id = io.dolby.uikit.R.drawable.icon_live_indicator),
                checked = showLiveIndicator.value,
                isEnabled = true,
                onCheckedChange = { viewModel.updateShowLiveIndicator(it) }
            )
        }
    }
}
