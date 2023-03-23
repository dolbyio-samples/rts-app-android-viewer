package io.dolby.rtsviewer.ui.streaming

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.dolby.rtsviewer.R
import io.dolby.rtsviewer.uikit.button.StyledIconButton
import io.dolby.rtsviewer.utils.anyDpadKeyEvent
import java.util.Locale

@Composable
fun StreamingToolbarView(viewModel: StreamingViewModel, showSettings: MutableState<Boolean>) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val showToolbarState by viewModel.showToolbarState.collectAsStateWithLifecycle()
    Box(
        modifier = Modifier
            .fillMaxSize()
            .focusable()
            .anyDpadKeyEvent { viewModel.showToolbar() }
    ) {
        ConstraintLayout(
            modifier = Modifier
                .fillMaxSize()
        ) {
            val (liveIndicator, toolbar, settings) = createRefs()
            val liveIndicatorLabel =
                if (uiState.subscribed) stringResource(R.string.live_label)
                else stringResource(R.string.offline_label)
            val liveIndicatorBackgroundColor =
                if (uiState.subscribed) MaterialTheme.colors.error else MaterialTheme.colors.surface

            if (uiState.showLiveIndicator) {
                Text(
                    text = liveIndicatorLabel.uppercase(Locale.ROOT),
                    style = MaterialTheme.typography.caption,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colors.onPrimary,
                    modifier = Modifier
                        .constrainAs(liveIndicator) {
                            top.linkTo(parent.top, margin = 14.dp)
                            start.linkTo(parent.start, margin = 20.dp)
                        }
                        .background(liveIndicatorBackgroundColor, shape = RoundedCornerShape(2.dp))
                        .padding(horizontal = 10.dp, vertical = 3.dp)
                )
            }
            val focusRequester = remember { FocusRequester() }
            AnimatedVisibility(
                visible = showToolbarState,
                modifier = Modifier
                    .constrainAs(toolbar) {
                        bottom.linkTo(parent.bottom)
                        end.linkTo(parent.end)
                    }
            ) {
                LaunchedEffect(Unit) {
                    focusRequester.requestFocus()
                }
                StyledIconButton(
                    modifier = Modifier
                        .constrainAs(settings) {
                            bottom.linkTo(parent.bottom, margin = 14.dp)
                            end.linkTo(parent.end, margin = 20.dp)
                        }
                        .focusRequester(focusRequester),
                    iconRes = io.dolby.uikit.R.drawable.ic_settings,
                    text = stringResource(id = R.string.settings_title),
                    contentDescription = stringResource(
                        id = R.string.settings_contentDescription
                    ),
                    onClick = {
                        if (showToolbarState) {
                            viewModel.hideToolbar()
                            showSettings.value = true
                        }
                    }
                )
            }
        }
    }
}
