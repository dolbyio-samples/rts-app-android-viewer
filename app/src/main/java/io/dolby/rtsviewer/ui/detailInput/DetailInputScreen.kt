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
import io.dolby.rtsviewer.ui.alert.AlertTypes
import io.dolby.rtsviewer.ui.alert.DetailInputConnectionAlert
import io.dolby.rtsviewer.ui.alert.ClearStreamConfirmationAlert
import io.dolby.rtsviewer.ui.alert.DetailInputValidationAlert
import io.dolby.rtsviewer.ui.alert.GenericAlert
import io.dolby.rtsviewer.uikit.button.ButtonType
import io.dolby.rtsviewer.uikit.button.StyledButton
import io.dolby.rtsviewer.uikit.input.TvTextInput
import io.dolby.rtsviewer.uikit.text.Text
import io.dolby.rtsviewer.uikit.theme.fontColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun DetailInputScreen(
    onPlayClick: (StreamingData) -> Unit,
    onPlayFromConfigClick: () -> Unit,
    onSavedStreamsClick: () -> Unit,
    modifier: Modifier = Modifier,
    streamingData: StreamingData? = null,
    viewModel: DetailInputViewModel = hiltViewModel()
) {
    var showMissingStreamDetailDialog by remember { mutableStateOf(false) }
    var showStreamConnectionErrorDialog by remember { mutableStateOf(false) }
    var showClearStreamsConfirmationDialog by remember { mutableStateOf(false) }
    var showFetchErrorDialog by remember { mutableStateOf(false) }

    val streamName = viewModel.streamName.collectAsState()
    val accountId = viewModel.accountId.collectAsState()
    val remoteConfigUrl = viewModel.remoteConfigUrl.collectAsState()

    val screenName = stringResource(id = R.string.stream_detail_screen_name)
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val localFocusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    val background = MaterialTheme.colors.background
    val coroutineScope = rememberCoroutineScope()

    val env = viewModel.listOfEnv()
    var selectedEnv by remember { mutableStateOf(env[0]) }

    fun playStream() {
        if (!viewModel.shouldPlayStream) {
            showMissingStreamDetailDialog = true
        } else {
            coroutineScope.launch(Dispatchers.Main) {
                val connected = viewModel.connect(selectedEnv)
                if (connected) {
                    onPlayClick(
                        StreamingData(
                            streamName = streamName.value,
                            accountId = accountId.value
                        )
                    )
                } else {
                    showStreamConnectionErrorDialog = true
                }
            }
        }
    }

    fun playStreamFromConfig() {
        coroutineScope.launch(Dispatchers.Main) {
            onPlayFromConfigClick()
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    LaunchedEffect(Unit) {
        streamingData?.let {
            viewModel.updateStreamName(it.streamName)
            viewModel.updateAccountId(it.accountId)
            playStream()
        }
    }

    if (showMissingStreamDetailDialog) {
        DetailInputValidationAlert(
            onDismiss = { showMissingStreamDetailDialog = false },
            modifier = modifier
        )
    }

    if (showStreamConnectionErrorDialog) {
        DetailInputConnectionAlert(
            onDismiss = { showStreamConnectionErrorDialog = false },
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

    if(showFetchErrorDialog) {
        GenericAlert(onDismiss = {
            showFetchErrorDialog = false
        }, alertType = AlertTypes.RemoteConfigFetchAlert)
    }

    when (uiState.remoteConfigFetchState) {
        RemoteConfigFetchState.IDLE -> {}
        RemoteConfigFetchState.FETCHING -> {}
        RemoteConfigFetchState.ERROR -> {
            viewModel.updateRemoteConfigFetchState(RemoteConfigFetchState.IDLE)
            showFetchErrorDialog = true
        }
        RemoteConfigFetchState.SUCCESS -> {
            viewModel.updateRemoteConfigFetchState(RemoteConfigFetchState.IDLE)
            playStreamFromConfig()
        }
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

                DetailInput(
                    accountId = accountId,
                    streamName = streamName,
                    viewModel = viewModel,
                    localFocusManager = localFocusManager,
                    focusRequester = focusRequester
                ) { playStream() }

                Spacer(modifier = modifier.height(8.dp))

                StyledButton(
                    buttonText = stringResource(id = R.string.env) + "- $selectedEnv",
                    onClickAction = {
                        val idx = env.indexOf(selectedEnv)
                        selectedEnv = env[(idx + 1) % env.size]
                    },
                    buttonType = ButtonType.SECONDARY
                )

                Spacer(modifier = modifier.height(8.dp))

                StyledButton(
                    buttonText = stringResource(id = R.string.play_button),
                    onClickAction = {
                        playStream()
                    },
                    buttonType = ButtonType.PRIMARY
                )

                Spacer(modifier = modifier.height(8.dp))

                StyledButton(
                    buttonText = stringResource(id = R.string.demo_stream_title),
                    onClickAction = {
                        viewModel.useDemoStream()
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

                if (viewModel.isAminoDevice) {
                    Spacer(modifier = modifier.height(8.dp))

                    StyledButton(
                        buttonText = stringResource(id = R.string.play_amino_button),
                        onClickAction = {
                            playStreamFromConfig()
                        },
                        buttonType = ButtonType.PRIMARY
                    )
                } else {
                    Spacer(modifier = modifier.height(8.dp))

                    TvTextInput(
                        value = remoteConfigUrl.value,
                        label = stringResource(id = R.string.remote_config_url),
                        onValueChange = {
                            viewModel.updateStreamName(it)
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(
                            onNext = { localFocusManager.moveFocus(FocusDirection.Down) }
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    StyledButton(
                        buttonText = stringResource(id = R.string.play_config_button),
                        onClickAction = {
                            viewModel.getRemoteConfig()
                        },
                        buttonType = ButtonType.PRIMARY
                    )
                }
            }
        }
    }
}
