package io.dolby.interactiveplayer.detailInput

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import io.dolby.interactiveplayer.R
import io.dolby.interactiveplayer.alert.ClearStreamConfirmationAlert
import io.dolby.interactiveplayer.alert.DetailInputValidationAlert
import io.dolby.interactiveplayer.rts.domain.StreamingData
import io.dolby.interactiveplayer.rts.ui.DolbyCopyrightFooterView
import io.dolby.interactiveplayer.rts.ui.TopAppBar
import io.dolby.interactiveplayer.utils.horizontalPaddingDp
import io.dolby.rtsviewer.uikit.button.ButtonType
import io.dolby.rtsviewer.uikit.button.StyledButton
import io.dolby.rtsviewer.uikit.theme.fontColor
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

    val streamName = viewModel.streamName.collectAsState()
    val accountId = viewModel.accountId.collectAsState()

    val screenName = stringResource(id = R.string.stream_detail_screen_name)

    val localFocusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    val background = MaterialTheme.colors.background
    val coroutineScope = rememberCoroutineScope()

    fun playStream() {
        if (!viewModel.shouldPlayStream) {
            showMissingStreamDetailDialog = true
        } else {
            viewModel.connect()

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
            TopAppBar(title = "", onBack = onBack, onAction = onSettingsClick)
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

                StyledButton(
                    buttonText = stringResource(id = R.string.play_button),
                    onClickAction = {
                        playStream()
                    },
                    buttonType = ButtonType.PRIMARY
                )

                Spacer(modifier = modifier.height(8.dp))

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

                Spacer(modifier = modifier.height(8.dp))

                StyledButton(
                    buttonText = "$DEMO_STREAM_NAME / ID $DEMO_ACCOUNT_ID",
                    onClickAction = {
                        viewModel.useDemoStream()
                        playStream()
                    },
                    buttonType = ButtonType.BASIC,
                    capitalize = false
                )
            }
        }
    }
}
