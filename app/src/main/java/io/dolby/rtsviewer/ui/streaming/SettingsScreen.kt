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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.dolby.rtsviewer.R
import io.dolby.rtsviewer.uikit.button.StateButton
import io.dolby.rtsviewer.uikit.switch.SwitchComponent
import io.dolby.rtsviewer.uikit.theme.DarkThemeColors

@Composable
fun SettingsScreen(viewModel: StreamingViewModel, showStatistics: MutableState<Boolean>) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = .75f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .width(325.dp)
                .align(Alignment.TopEnd)
                .background(DarkThemeColors().neutralColor800)
                .padding(vertical = 42.dp)
                .padding(start = 25.dp, end = 22.dp)
        ) {
            StateButton(
                text = stringResource(id = R.string.simulcast_title),
                icon = painterResource(id = io.dolby.uikit.R.drawable.icon_simulcast),
                stateText = "Auto",
                isEnabled = true,
                onClick = { /*TODO*/ }
            )
            Spacer(modifier = Modifier.height(12.dp))
            SwitchComponent(
                modifier = Modifier.focusRequester(focusRequester),
                text = stringResource(id = R.string.streaming_statistics_title),
                icon = painterResource(id = io.dolby.uikit.R.drawable.icon_info),
                checked = showStatistics.value,
                isEnabled = true,
                onCheckChange = {
                    showStatistics.value = it
                }
            )
            Spacer(modifier = Modifier.height(12.dp))
            SwitchComponent(
                text = stringResource(id = R.string.live_indicator_title),
                icon = painterResource(id = io.dolby.uikit.R.drawable.icon_live_indicator),
                checked = uiState.showLiveIndicator,
                isEnabled = true,
                onCheckChange = { viewModel.updateShowLiveIndicator(it) }
            )
        }
    }
}
