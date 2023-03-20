package io.dolby.rtsviewer.ui.detailInput

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.Button
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
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import io.dolby.rtscomponentkit.domain.StreamingData
import io.dolby.rtscomponentkit.ui.DolbyBackgroundBox
import io.dolby.rtscomponentkit.ui.DolbyCopyrightFooterView
import io.dolby.rtscomponentkit.ui.TopActionBar
import io.dolby.rtsviewer.R
import io.dolby.rtsviewer.uikit.button.StyledButton
import io.dolby.rtsviewer.uikit.input.TextInput
import io.dolby.rtsviewer.uikit.theme.fontColor
import io.dolby.rtsviewer.uikit.utils.ViewState

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun DetailInputScreen(
    onPlayClick: (StreamingData) -> Unit,
    onSavedStreamsClick: () -> Unit,
    viewModel: DetailInputViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    var streamName by remember { mutableStateOf("") }
    var accountId by remember { mutableStateOf("") }
    var showMissingStreamDetailDialog by remember { mutableStateOf(false) }
    val screenName = stringResource(id = R.string.stream_detail_screen_name)

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
                    Button(
                        modifier = Modifier
                            .width(200.dp),
                        onClick = { showMissingStreamDetailDialog = false }
                    ) {
                        Text(stringResource(id = R.string.missing_stream_detail_dismiss_button))
                    }
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
    ) { _ ->
        DolbyBackgroundBox(
            modifier = modifier
                .semantics { contentDescription = screenName }
                .verticalScroll(rememberScrollState())
                .fillMaxSize()
        ) {
            val localFocusManager = LocalFocusManager.current
            val focusRequester = remember { FocusRequester() }

            LaunchedEffect(Unit) {
                focusRequester.requestFocus()
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = modifier
                    .verticalScroll(rememberScrollState())
                    .width(450.dp)
                    .align(Alignment.Center)
                    .background(MaterialTheme.colors.background, shape = RoundedCornerShape(4.dp))
                    .clip(MaterialTheme.shapes.large)
                    .padding(horizontal = 55.dp)
                    .padding(vertical = 16.dp)
                    .onKeyEvent {
                        if (it.type == KeyEventType.KeyUp && it.key == Key.DirectionDown) {
                            localFocusManager.moveFocus(FocusDirection.Down)
                            return@onKeyEvent true
                        } else if (it.type == KeyEventType.KeyUp && it.key == Key.DirectionUp) {
                            localFocusManager.moveFocus(FocusDirection.Up)
                            return@onKeyEvent true
                        }
                        return@onKeyEvent true
                    }
            ) {
                Text(
                    stringResource(id = R.string.stream_detail_header),
                    style = MaterialTheme.typography.body1,
                    fontWeight = FontWeight.Medium,
                    color = fontColor(ViewState.Selected),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = modifier.height(12.dp))

                Text(
                    stringResource(id = R.string.stream_detail_title),
                    style = MaterialTheme.typography.h2,
                    color = fontColor(ViewState.Selected),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = modifier.height(5.dp))

                Text(
                    stringResource(id = R.string.stream_detail_subtitle),
                    style = MaterialTheme.typography.body2,
                    color = fontColor(ViewState.Selected),
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
                    modifier = modifier.focusRequester(focusRequester)
                )

                Spacer(modifier = modifier.height(8.dp))

                TextInput(
                    value = accountId,
                    label = stringResource(id = R.string.account_id_placeholder),
                    onValueChange = {
                        accountId = it
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = { localFocusManager.clearFocus() }
                    )
                )

                Spacer(modifier = modifier.height(8.dp))

                StyledButton(
                    buttonText = stringResource(id = R.string.saved_streams_button),
                    onClickAction = {
                        onSavedStreamsClick()
                    },
                    isPrimary = false
                )

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

                Row(
                    modifier = modifier
                        .fillMaxWidth()
                ) {
                    TextButton(
                        onClick = { /* Do something! */ }
                    ) {
                        Text(
                            stringResource(id = R.string.clear_stream_history_button),
                            color = MaterialTheme.colors.onSecondary
                        )
                    }
                }
            }
        }
    }
}
