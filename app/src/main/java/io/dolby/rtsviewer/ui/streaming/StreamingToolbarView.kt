package io.dolby.rtsviewer.ui.streaming

import android.view.KeyEvent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import io.dolby.rtsviewer.uikit.text.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.nativeKeyCode
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.dolby.rtsviewer.R
import io.dolby.rtsviewer.uikit.button.StyledIconButton
import io.dolby.rtsviewer.utils.anyDpadKeyEvent
import kotlinx.coroutines.delay
import java.util.Locale

@Composable
fun StreamingToolbarView(viewModel: StreamingViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val showLiveIndicator by viewModel.showLiveIndicator.collectAsStateWithLifecycle()
    val showToolbarState by viewModel.showToolbarState.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .focusable()
            .anyDpadKeyEvent { viewModel.showToolbar() }
            .onKeyEvent {
                if (showToolbarState && it.key.nativeKeyCode == KeyEvent.KEYCODE_BACK) {
                    viewModel.hideToolbar()
                    return@onKeyEvent true
                }
                return@onKeyEvent false
            }
    ) {
        val toolbarContentDescription =
            stringResource(id = R.string.streamingToolbar_contentDescription)
        ConstraintLayout(
            modifier = Modifier
                .fillMaxSize()
                .semantics { contentDescription = toolbarContentDescription }
        ) {
            val (liveIndicator, toolbar, settings) = createRefs()
            if (showLiveIndicator) {
                val liveIndicatorLabel =
                    if (uiState.subscribed) stringResource(R.string.live_label)
                    else stringResource(R.string.offline_label)
                val liveIndicatorBackgroundColor =
                    if (uiState.subscribed) MaterialTheme.colors.error else MaterialTheme.colors.surface
                val liveIndicatorContentDescription =
                    "$liveIndicatorLabel ${stringResource(id = R.string.liveIndicator_contentDescription)}"
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
                        .semantics { contentDescription = liveIndicatorContentDescription }
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
                    .semantics { contentDescription = "visibility button" }
            ) {
                LaunchedEffect(Unit) {
                    delay(200)
                    focusRequester.requestFocus()
                }
                StyledIconButton(
                    modifier = Modifier
                        .constrainAs(settings) {
                            bottom.linkTo(parent.bottom, margin = 14.dp)
                            end.linkTo(parent.end, margin = 20.dp)
                        }
                        .focusRequester(focusRequester),
                    icon = painterResource(id = io.dolby.uikit.R.drawable.ic_settings),
                    text = stringResource(id = R.string.settings_title),
                    onClick = {
                        if (showToolbarState) {
                            viewModel.hideToolbar()
                            viewModel.settingsVisibility(true)
                        }
                    }
                )
            }
        }
    }
}
