package io.dolby.interactiveplayer.detailInput

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Card
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ExposedDropdownMenuBox
import androidx.compose.material.ExposedDropdownMenuDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Slider
import androidx.compose.material.Switch
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.dolby.interactiveplayer.R
import io.dolby.interactiveplayer.alert.ClearStreamConfirmationAlert
import io.dolby.interactiveplayer.alert.DetailInputValidationAlert
import io.dolby.interactiveplayer.rts.ui.DolbyCopyrightFooterView
import io.dolby.interactiveplayer.rts.ui.TopActionBar
import io.dolby.interactiveplayer.rts.ui.TopAppBar
import io.dolby.interactiveplayer.utils.horizontalPaddingDp
import io.dolby.rtscomponentkit.domain.ConnectOptions
import io.dolby.rtscomponentkit.domain.StreamingData
import io.dolby.rtsviewer.uikit.button.ButtonType
import io.dolby.rtsviewer.uikit.button.StyledButton
import io.dolby.rtsviewer.uikit.text.Text
import io.dolby.rtsviewer.uikit.theme.fontColor
import io.dolby.rtsviewer.uikit.utils.buttonHeight
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun DetailInputScreen(
    onPlayClick: (StreamingData) -> Unit,
    onSettingsClick: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    streamingData: StreamingData? = null,
    viewModel: DetailInputViewModel = hiltViewModel()
) {
    var showMissingStreamDetailDialog by remember { mutableStateOf(false) }
    var showClearStreamsConfirmationDialog by remember { mutableStateOf(false) }

    val savedStreamsUiState by viewModel.uiState.collectAsStateWithLifecycle()
    val streamName = viewModel.streamName.collectAsState()
    val accountId = viewModel.accountId.collectAsState()
    val recentStreams = viewModel.uiState.collectAsState()

    val showDebugOptions = viewModel.showDebugOptions.collectAsState()

    val screenName = stringResource(id = R.string.stream_detail_screen_name)

    val localFocusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    val background = MaterialTheme.colors.background
    val coroutineScope = rememberCoroutineScope()

    fun playStream() {
        if (!viewModel.shouldPlayStream) {
            showMissingStreamDetailDialog = true
        } else {
            viewModel.saveSelectedStream()

            coroutineScope.launch(Dispatchers.Main) {
                onPlayClick(
                    StreamingData(
                        streamName = streamName.value,
                        accountId = accountId.value
                    )
                )
            }
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
            if (savedStreamsUiState.recentStreams.isNotEmpty()) {
                TopAppBar(title = "", onBack = onBack, onAction = onSettingsClick)
            } else {
                TopActionBar(onActionClick = onSettingsClick)
            }
        },
        bottomBar = {
            DolbyCopyrightFooterView()
        },
        modifier = modifier
    ) { paddingValues ->
        Box(
            modifier = modifier
                .padding(paddingValues)
                .semantics { contentDescription = screenName }
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = modifier
                    .verticalScroll(rememberScrollState())
                    .fillMaxWidth()
                    .align(Alignment.Center)
                    .background(background, shape = RoundedCornerShape(4.dp))
                    .clip(MaterialTheme.shapes.large)
                    .padding(horizontal = horizontalPaddingDp(), vertical = 16.dp)
            ) {
                Text(
                    stringResource(id = R.string.stream_detail_header),
                    style = MaterialTheme.typography.h2,
                    fontWeight = FontWeight.Medium,
                    color = fontColor(background),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = modifier.height(16.dp))

                Text(
                    stringResource(id = R.string.stream_detail_title),
                    style = MaterialTheme.typography.h3,
                    color = fontColor(background),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = modifier.height(8.dp))

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

                Spacer(modifier = modifier.height(12.dp))

                if (showDebugOptions.value) {
                    ConnectionOptions(viewModel)
                }

                StyledButton(
                    buttonText = stringResource(id = R.string.play_button),
                    onClickAction = {
                        playStream()
                    },
                    buttonType = ButtonType.PRIMARY
                )

                Spacer(modifier = modifier.height(16.dp))

                Text(
                    stringResource(id = R.string.demo_stream_title),
                    style = MaterialTheme.typography.h3,
                    color = fontColor(background),
                    textAlign = TextAlign.Center
                )

                Text(
                    stringResource(id = R.string.demo_stream_subtitle),
                    style = MaterialTheme.typography.body2,
                    color = fontColor(background),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = modifier.height(12.dp))
                val demoConnectionOptions = if (showDebugOptions.value) {
                    val recentDemoStream =
                        recentStreams.value.recentStreams.firstOrNull { it.accountID == DEMO_ACCOUNT_ID && it.streamName == DEMO_STREAM_NAME }
                    val connectOptions =
                        recentDemoStream?.let {
                            ConnectOptions.from(
                                it.useDevEnv,
                                it.forcePlayOutDelay,
                                it.disableAudio,
                                it.rtcLogs,
                                it.primaryVideoQuality,
                                it.videoJitterMinimumDelayMs
                            )
                        } ?: ConnectOptions()
                    connectionOptionsText(connectOptions)
                } else null
                StyledButton(
                    buttonText = DEMO_STREAM_NAME,
                    subtextTitle = stringResource(id = R.string.id_title),
                    subtext = DEMO_ACCOUNT_ID,
                    moreTexts = demoConnectionOptions,
                    onClickAction = {
                        viewModel.useDemoStream()
                        playStream()
                    },
                    buttonType = ButtonType.BASIC,
                    capitalize = false,
                    endIcon = painterResource(id = io.dolby.uikit.R.drawable.ic_play_outlined)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ConnectionOptions(viewModel: DetailInputViewModel) {
    val selectedConnectOptions = viewModel.selectedConnectionOptions.collectAsState()
    var isExpanded by rememberSaveable { mutableStateOf(false) }

    Card(
        modifier = Modifier.clickable { isExpanded = !isExpanded }
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .buttonHeight(ButtonType.PRIMARY)
                    .padding(8.dp)
            ) {
                Text(
                    stringResource(id = R.string.connection_options_more_stream_config_title),
                    textAlign = TextAlign.Start
                )
                Spacer(modifier = Modifier.weight(1f))
                Image(
                    painter = painterResource(
                        id = if (isExpanded) {
                            io.dolby.uikit.R.drawable.ic_arrow_down_24
                        } else io.dolby.uikit.R.drawable.ic_arrow_right_24
                    ),
                    contentDescription = stringResource(
                        id = R.string.connection_options_more_stream_config_title
                    )
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(id = R.string.stream_connection_options_dev_server_title))
                        Spacer(modifier = Modifier.weight(1.0f))
                        EnvSelection(viewModel = viewModel)
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(id = R.string.stream_connection_options_force_playout_delay_title))
                        Spacer(modifier = Modifier.weight(1.0f))
                        Switch(
                            checked = selectedConnectOptions.value.forcePlayOutDelay,
                            onCheckedChange = { viewModel.updateForcePlayOutDelay(it) }
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(id = R.string.stream_connection_options_disable_audio_title))
                        Spacer(modifier = Modifier.weight(1.0f))
                        Switch(
                            checked = selectedConnectOptions.value.disableAudio,
                            onCheckedChange = { viewModel.updateDisableAudio(it) }
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(id = R.string.stream_connection_options_save_logs_title))
                        Spacer(modifier = Modifier.weight(1.0f))
                        Switch(
                            checked = selectedConnectOptions.value.rtcLogs,
                            onCheckedChange = { viewModel.updateRtcLogs(it) }
                        )
                    }

                    var sliderValue by remember { mutableFloatStateOf(selectedConnectOptions.value.videoJitterMinimumDelayMs.toFloat()) }
                    Text(
                        text = stringResource(id = R.string.connection_options_jitter_buffer_delay_title) +
                                " - ${selectedConnectOptions.value.videoJitterMinimumDelayMs}ms"
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "0",
                            overflow = TextOverflow.Visible,
                            maxLines = 1
                        )

                        Spacer(modifier = Modifier.width(5.dp))

                        Slider(
                            value = sliderValue,
                            valueRange = 0f..2000f,
                            steps = (2000 / 50) - 1,
                            onValueChange = {
                                sliderValue = it
                                viewModel.updateJitterBufferMinimumDelay(it.toInt())
                            },
                            modifier = Modifier.weight(1f)
                        )

                        Spacer(modifier = Modifier.width(5.dp))

                        Text(
                            "2sec",
                            overflow = TextOverflow.Visible,
                            maxLines = 1
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(stringResource(id = R.string.stream_connection_options_primary_video_quality_title))
                        Spacer(modifier = Modifier.weight(1.0f))
                        QualityLabel(viewModel = viewModel)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun EnvSelection(viewModel: DetailInputViewModel) {
    var envMenuExpanded by remember { mutableStateOf(false) }
    val env = viewModel.listOfEnv()
    var selectedEnv by remember { mutableStateOf(env[0]) }

    ExposedDropdownMenuBox(
        expanded = envMenuExpanded,
        onExpandedChange = {
            envMenuExpanded = !envMenuExpanded
        }
    ) {
        TextField(
            readOnly = true,
            value = selectedEnv.name,
            onValueChange = { },
            // label = { Text(stringResource(id = R.string.stream_connection_options_dev_server_title)) },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(
                    expanded = envMenuExpanded
                )
            },
            colors = ExposedDropdownMenuDefaults.textFieldColors()
        )
        ExposedDropdownMenu(
            expanded = envMenuExpanded,
            onDismissRequest = {
                envMenuExpanded = false
            }
        ) {
            env.forEach { selectionOption ->
                DropdownMenuItem(
                    onClick = {
                        selectedEnv = selectionOption
                        envMenuExpanded = false
                    }
                ) {
                    Text(text = selectionOption.name)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun QualityLabel(viewModel: DetailInputViewModel) {
    val selectedConnectionOptions = viewModel.selectedConnectionOptions.collectAsState()
    val showVideoQualitySelection = viewModel.showVideoQualityState.collectAsStateWithLifecycle()
    Row {
        ExposedDropdownMenuBox(
            expanded = showVideoQualitySelection.value,
            onExpandedChange = { viewModel.showPrimaryVideoQualitySelection(it) }
        ) {
            TextField(
                value = selectedConnectionOptions.value.primaryVideoQuality.name,
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showVideoQualitySelection.value) }
            )
            ExposedDropdownMenu(
                expanded = showVideoQualitySelection.value,
                onDismissRequest = { viewModel.showPrimaryVideoQualitySelection(false) }
            ) {
                viewModel.videoQualities().forEach { item ->
                    DropdownMenuItem(onClick = {
                        viewModel.updatePrimaryVideoQuality(item)
                        viewModel.showPrimaryVideoQualitySelection(false)
                    }) {
                        Text(text = item.name)
                    }
                }
            }
        }
    }
}

@Composable
fun connectionOptionsText(connectOptions: ConnectOptions) =
    "${stringResource(id = R.string.stream_connection_options_dev_server_title)} ${connectOptions.useDevEnv}\n" +
            "${stringResource(id = R.string.stream_connection_options_video_jitter_buffer_ms_title)} ${connectOptions.videoJitterMinimumDelayMs}\n" +
            "${stringResource(id = R.string.stream_connection_options_force_playout_delay_title)} ${connectOptions.forcePlayOutDelay}\n" +
            "${stringResource(id = R.string.stream_connection_options_disable_audio_title)} ${connectOptions.disableAudio}\n" +
            "${stringResource(id = R.string.stream_connection_options_primary_video_quality_title)} ${connectOptions.primaryVideoQuality}\n" +
            "${stringResource(id = R.string.stream_connection_options_save_logs_title)} ${connectOptions.rtcLogs}"
