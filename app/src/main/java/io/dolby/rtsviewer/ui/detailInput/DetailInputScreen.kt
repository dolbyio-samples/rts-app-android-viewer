package io.dolby.rtsviewer.ui.detailInput

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.AlertDialog
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.nativeKeyCode
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.dolby.rtscomponentkit.domain.StreamingData
import io.dolby.rtscomponentkit.ui.DolbyBackgroundBox
import io.dolby.rtscomponentkit.ui.DolbyCopyrightFooterView
import io.dolby.rtscomponentkit.ui.TopActionBar
import io.dolby.rtsviewer.R
import io.dolby.rtsviewer.uikit.button.StyledButton
import io.dolby.rtsviewer.uikit.input.TextInput
import io.dolby.rtsviewer.uikit.theme.fontColor

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun DetailInputScreen(
    onPlayClick: (StreamingData) -> Unit,
    onSavedStreamsClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DetailInputViewModel = hiltViewModel()
) {
    var streamName by remember { mutableStateOf("") }
    var accountId by remember { mutableStateOf("") }
    var showMissingStreamDetailDialog by remember { mutableStateOf(false) }
    val screenName = stringResource(id = R.string.stream_detail_screen_name)
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    if (showMissingStreamDetailDialog) {
        AlertDialog(
            onDismissRequest = {
                showMissingStreamDetailDialog = false
            },
            text = {
                Text(text = stringResource(id = R.string.missing_stream_name_or_account_id))
            },
            buttons = {
                Column(
                    modifier = Modifier
                        .padding(all = 8.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    StyledButton(
                        buttonText = stringResource(id = R.string.missing_stream_detail_dismiss_button),
                        onClickAction = { showMissingStreamDetailDialog = false },
                        isPrimary = false,
                        modifier = Modifier
                            .width(200.dp)
                    )
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopActionBar()
        },
        bottomBar = {
            DolbyCopyrightFooterView()
        },
        modifier = modifier
    ) { paddingValues ->
        DolbyBackgroundBox(
            modifier = modifier
                .padding(paddingValues)
                .semantics { contentDescription = screenName }
        ) {
            val localFocusManager = LocalFocusManager.current
            val focusRequester = remember { FocusRequester() }
            val background = MaterialTheme.colors.background
            LaunchedEffect(Unit) {
                focusRequester.requestFocus()
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = modifier
                    .verticalScroll(rememberScrollState())
                    .width(450.dp)
                    .align(Alignment.Center)
                    .background(background, shape = RoundedCornerShape(4.dp))
                    .clip(MaterialTheme.shapes.large)
                    .padding(horizontal = 55.dp)
                    .padding(vertical = 16.dp)
                    .onKeyEvent {
                        if (it.key.nativeKeyCode == android.view.KeyEvent.KEYCODE_DPAD_DOWN) {
                            if (it.nativeKeyEvent.action == android.view.KeyEvent.ACTION_UP) {
                                localFocusManager.moveFocus(FocusDirection.Down)
                                return@onKeyEvent true
                            } else if (it.nativeKeyEvent.action == android.view.KeyEvent.ACTION_DOWN) {
                                // Bypass the event
                                return@onKeyEvent true
                            }
                        } else if (it.key.nativeKeyCode == android.view.KeyEvent.KEYCODE_DPAD_UP) {
                            if (it.nativeKeyEvent.action == android.view.KeyEvent.ACTION_UP) {
                                localFocusManager.moveFocus(FocusDirection.Up)
                                return@onKeyEvent true
                            } else if (it.nativeKeyEvent.action == android.view.KeyEvent.ACTION_DOWN) {
                                // Bypass the event
                                return@onKeyEvent true
                            }
                        }
                        return@onKeyEvent false
                    }
            ) {
                Text(
                    stringResource(id = R.string.stream_detail_header),
                    style = MaterialTheme.typography.body1,
                    fontWeight = FontWeight.Medium,
                    color = fontColor(background),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = modifier.height(12.dp))

                Text(
                    stringResource(id = R.string.stream_detail_title),
                    style = MaterialTheme.typography.h2,
                    color = fontColor(background),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = modifier.height(5.dp))

                Text(
                    stringResource(id = R.string.stream_detail_subtitle),
                    style = MaterialTheme.typography.body2,
                    color = fontColor(background),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = modifier.height(12.dp))

                TextInput(
                    value = streamName,
                    label = stringResource(id = R.string.stream_name_placeholder),
                    onValueChange = {
                        streamName = it
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(
                        onNext = { localFocusManager.moveFocus(FocusDirection.Down) }
                    ),
                    modifier = Modifier.focusRequester(focusRequester)
                )

                Spacer(modifier = modifier.height(8.dp))

                TextInput(
                    value = accountId,
                    label = stringResource(id = R.string.account_id_placeholder),
                    onValueChange = {
                        accountId = it
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(
                        onNext = { localFocusManager.moveFocus(FocusDirection.Down) }
                    )
                )

                Spacer(modifier = modifier.height(8.dp))

                if (uiState.recentStreams.isNotEmpty()) {
                    StyledButton(
                        buttonText = stringResource(id = R.string.saved_streams_button),
                        onClickAction = {
                            onSavedStreamsClick()
                        },
                        isPrimary = false
                    )
                }

                Spacer(modifier = modifier.height(8.dp))

                StyledButton(
                    buttonText = stringResource(id = R.string.play_button),
                    onClickAction = {
                        if (streamName.isEmpty() || accountId.isEmpty()) {
                            showMissingStreamDetailDialog = true
                        } else {
                            viewModel.connect(streamName = streamName, accountId = accountId)
                            onPlayClick(
                                StreamingData(
                                    streamName = streamName,
                                    accountId = accountId
                                )
                            )
                        }
                    },
                    isPrimary = true
                )

                if (uiState.recentStreams.isNotEmpty()) {
                    Row(
                        modifier = modifier
                            .fillMaxWidth()
                    ) {
                        TextButton(
                            onClick = { /* Do something! */ }
                        ) {
                            Text(
                                stringResource(id = R.string.clear_stream_history_button),
                                color = MaterialTheme.colors.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}
