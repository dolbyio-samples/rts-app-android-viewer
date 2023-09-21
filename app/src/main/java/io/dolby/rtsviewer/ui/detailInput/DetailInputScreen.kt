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
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Slider
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.dolby.rtscomponentkit.domain.StreamingData
import io.dolby.rtscomponentkit.ui.DolbyBackgroundBox
import io.dolby.rtscomponentkit.ui.DolbyCopyrightFooterView
import io.dolby.rtscomponentkit.ui.TopActionBar
import io.dolby.rtsviewer.R
import io.dolby.rtsviewer.ui.alert.ClearStreamConfirmationAlert
import io.dolby.rtsviewer.ui.alert.DetailInputValidationAlert
import io.dolby.rtsviewer.uikit.button.ButtonType
import io.dolby.rtsviewer.uikit.button.StyledButton
import io.dolby.rtsviewer.uikit.input.TextInput
import io.dolby.rtsviewer.uikit.input.TvTextInput
import io.dolby.rtsviewer.uikit.theme.fontColor
import io.dolby.rtsviewer.utils.isTV
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private const val MAXIMUM_CHARACTERS: Int = 64

@Composable
fun DetailInputScreen(
    onPlayClick: (StreamingData) -> Unit,
    onSavedStreamsClick: () -> Unit,
    modifier: Modifier = Modifier,
    initialStreamName: String? = null,
    viewModel: DetailInputViewModel = hiltViewModel()
) {
    var showMissingStreamDetailDialog by remember { mutableStateOf(false) }
    var showClearStreamsConfirmationDialog by remember { mutableStateOf(false) }

    val streamName = viewModel.streamName.collectAsState()
    val accountId = viewModel.accountId.collectAsState()

    val screenName = stringResource(id = R.string.stream_detail_screen_name)
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val localFocusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    val background = MaterialTheme.colors.background
    val coroutineScope = rememberCoroutineScope()

    val useDevEnv = viewModel.useDevEnv.collectAsState()
    val forcePlayOutDelay = viewModel.forcePlayOutDelay.collectAsState()
    val disableAudio = viewModel.disableAudio.collectAsState()
    val rtcLogs = viewModel.rtcLogs.collectAsState()
    val videoJitterMinimumDelayMs = viewModel.videoJitterMinimumDelayMs.collectAsState()
    var sliderValue by remember { mutableStateOf(0f) }

    fun playStream() {
        if (!viewModel.shouldPlayStream) {
            showMissingStreamDetailDialog = true
        } else {
            val sd = viewModel.connect()
            coroutineScope.launch(Dispatchers.Main) {
                onPlayClick(sd)
                viewModel.resetStreamIfDemo()
            }
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    LaunchedEffect(Unit) {
        initialStreamName?.let {
            viewModel.useStreamingData(it)
        }
    }

    LaunchedEffect(Unit) {
        sliderValue = videoJitterMinimumDelayMs.value
    }

    if (showMissingStreamDetailDialog) {
        DetailInputValidationAlert(
            onDismiss = { showMissingStreamDetailDialog = false },
            modifier = modifier
        )
    }

    if (showClearStreamsConfirmationDialog) {
        ClearStreamConfirmationAlert(
            onClear = {
                viewModel.clearAllStreams()
                showClearStreamsConfirmationDialog = false
            },
            onDismiss = {
                showClearStreamsConfirmationDialog = false
            },
            modifier = modifier
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
            ) {
                Text(
                    stringResource(id = R.string.app_name),
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

                if (isTV()) {
                    TvTextInput(
                        value = streamName.value,
                        label = stringResource(id = R.string.stream_name_placeholder),
                        onValueChange = {
                            viewModel.updateStreamName(it)
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(
                            onNext = { localFocusManager.moveFocus(FocusDirection.Down) }
                        ),
                        maximumCharacters = MAXIMUM_CHARACTERS,
                        modifier = Modifier.focusRequester(focusRequester)
                    )
                } else {
                    TextInput(
                        value = streamName.value,
                        label = stringResource(id = R.string.stream_name_placeholder),
                        onValueChange = {
                            viewModel.updateStreamName(it)
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(
                            onNext = { localFocusManager.moveFocus(FocusDirection.Down) }
                        ),
                        maximumCharacters = MAXIMUM_CHARACTERS,
                        modifier = Modifier.focusRequester(focusRequester)
                    )
                }

                Spacer(modifier = modifier.height(8.dp))

                if (isTV()) {
                    TvTextInput(
                        value = accountId.value,
                        label = stringResource(id = R.string.account_id_placeholder),
                        onValueChange = {
                            viewModel.updateAccountId(it)
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(
                            onDone = { playStream() }
                        ),
                        maximumCharacters = MAXIMUM_CHARACTERS
                    )
                } else {
                    TextInput(
                        value = accountId.value,
                        label = stringResource(id = R.string.account_id_placeholder),
                        onValueChange = {
                            viewModel.updateAccountId(it)
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(
                            onDone = { playStream() }
                        ),
                        maximumCharacters = MAXIMUM_CHARACTERS
                    )

                    Spacer(modifier = modifier.height(16.dp))

                    Text(text = stringResource(id = R.string.jitter_buffer) + " - " + videoJitterMinimumDelayMs.value.toInt() + "ms")
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "0",
                            overflow = TextOverflow.Visible,
                            maxLines = 1
                        )

                        Spacer(modifier = modifier.width(5.dp))

                        Slider(
                            value = sliderValue,
                            valueRange = 0f..2000f,
                            steps = (2000 / 50) - 1,
                            onValueChange = {
                                sliderValue = it
                                viewModel.updateJitterBufferMinimumDelay(it)
                            },
                            modifier = modifier.width(220.dp)
                        )

                        Spacer(modifier = modifier.width(5.dp))

                        Text(
                            "2sec",
                            overflow = TextOverflow.Visible,
                            maxLines = 1
                        )
                    }

                }

                Column {
                    Row {
                        Column {
                            Text("Dev")
                            Switch(
                                checked = useDevEnv.value,
                                onCheckedChange = { viewModel.updateUseDevEnv(it) }
                            )
                        }
                        Spacer(modifier = modifier.weight(1.0f))
                        Column {
                            Text("No Playout Delay")
                            Switch(
                                checked = forcePlayOutDelay.value,
                                onCheckedChange = { viewModel.updateForcePlayOutDelay(it)}
                            )
                        }
                    }
                    Row {
                        Column {
                            Text("Disable Audio")
                            Switch(
                                checked = disableAudio.value,
                                onCheckedChange = { viewModel.updateDisableAudio(it) }
                            )
                        }
                        Spacer(modifier = modifier.weight(1.0f))
                        Column {
                            Text("RTC Logs")
                            Switch(
                                checked = rtcLogs.value,
                                onCheckedChange = { viewModel.updateRtcLogs(it) }
                            )
                        }
                    }
                }

                StyledButton(
                    buttonText = stringResource(id = R.string.play_button),
                    onClickAction = {
                        playStream()
                    },
                    buttonType = ButtonType.PRIMARY
                )

                Spacer(modifier = modifier.height(8.dp))

                if (uiState.recentStreams.isNotEmpty()) {
                    StyledButton(
                        buttonText = stringResource(id = R.string.saved_streams_button),
                        onClickAction = {
                            onSavedStreamsClick()
                        },
                        buttonType = ButtonType.SECONDARY
                    )
                }

                if (uiState.recentStreams.isNotEmpty()) {
                    Row(
                        modifier = modifier
                            .fillMaxWidth()
                    ) {
                        TextButton(
                            onClick = { showClearStreamsConfirmationDialog = true }
                        ) {
                            Text(
                                stringResource(id = R.string.clear_stream_history_button),
                                color = MaterialTheme.colors.onSurface
                            )
                        }
                    }
                }

                Spacer(modifier = modifier.height(8.dp))

                StyledButton(
                    buttonText = stringResource(id = R.string.demo_streams_button),
                    onClickAction = {
                        viewModel.useDemoStream()
                        playStream()
                    },
                    buttonType = ButtonType.SECONDARY
                )
            }
        }
    }
}
