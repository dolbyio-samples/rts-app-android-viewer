@file:OptIn(ExperimentalMaterialApi::class)

package io.dolby.interactiveplayer.savedStreams

import android.content.Context
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.material.DismissDirection
import androidx.compose.material.DismissValue
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.FractionalThreshold
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.SwipeToDismiss
import androidx.compose.material.Text
import androidx.compose.material.rememberDismissState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.dolby.interactiveplayer.R
import io.dolby.interactiveplayer.alert.ClearStreamConfirmationAlert
import io.dolby.interactiveplayer.datastore.StreamDetail
import io.dolby.interactiveplayer.rts.domain.StreamingData
import io.dolby.interactiveplayer.rts.ui.DolbyBackgroundBox
import io.dolby.interactiveplayer.rts.ui.TopAppBar
import io.dolby.interactiveplayer.utils.horizontalPaddingDp
import io.dolby.interactiveplayer.utils.streamingDataFrom
import io.dolby.rtsviewer.uikit.button.ButtonType
import io.dolby.rtsviewer.uikit.button.StyledButton
import io.dolby.rtsviewer.uikit.theme.fontColor
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SavedStreamScreen(
    modifier: Modifier = Modifier,
    onPlayStream: (StreamingData) -> Unit,
    onBack: () -> Unit,
    viewModel: SavedStreamViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val focusRequester = remember { FocusRequester() }
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val scope = rememberCoroutineScope()
    var lastPlayedStreamCoordinates by remember {
        mutableStateOf(Rect.Zero)
    }
    var showClearStreamsConfirmationDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(500)
        focusRequester.requestFocus()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = "Saved Streams",
                onBack = { onBack() },
                actionIcon = io.dolby.uikit.R.drawable.ic_delete,
                onAction = { showClearStreamsConfirmationDialog = true }
            )
        },
        modifier = modifier
    ) { paddingValues ->
        DolbyBackgroundBox(
            showGradientBackground = false,
            modifier = modifier.padding(paddingValues)
        ) {
            val background = MaterialTheme.colors.background

            LazyColumn(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(horizontal = horizontalPaddingDp(), vertical = 16.dp)
            ) {
                // Header - Title

                stickyHeader {
                    Text(
                        stringResource(id = R.string.saved_streams_screen_title),
                        style = MaterialTheme.typography.h2,
                        fontWeight = FontWeight.Bold,
                        color = fontColor(background),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                            .background(background)
                    )
                }

                // Section - Last played stream

                item {
                    Spacer(modifier = modifier.height(8.dp))

                    Text(
                        stringResource(id = R.string.saved_streams_section_header_lastPlayed),
                        style = MaterialTheme.typography.body1,
                        fontWeight = FontWeight.Normal,
                        color = fontColor(background),
                        textAlign = TextAlign.Left,
                        modifier = Modifier
                            .fillMaxWidth()
                    )

                    Spacer(modifier = modifier.height(8.dp))
                }

                if (uiState.recentStreams.isNotEmpty()) {
                    item {
                        val lastPlayedStream = uiState.recentStreams.first()
                        DismissibleRecentStream(
                            viewModel = viewModel,
                            streamDetail = lastPlayedStream,
                            onClickAction = {
                                onPlayStream(streamingDataFrom(lastPlayedStream))
                            },
                            modifier = Modifier
                                .bringIntoViewRequester(bringIntoViewRequester)
                                .onGloballyPositioned {
                                    lastPlayedStreamCoordinates = it.boundsInParent()
                                }
                                .focusRequester(focusRequester)
                                .onFocusEvent {
                                    if (it.isFocused) {
                                        scope.launch {
                                            bringIntoViewRequester.bringIntoView(
                                                lastPlayedStreamCoordinates.copy(
                                                    top = lastPlayedStreamCoordinates.top - 200F
                                                )
                                            )
                                        }
                                    }
                                }
                        )
                    }
                }

                // Section - All Streams

                item {
                    Spacer(modifier = modifier.height(16.dp))

                    Text(
                        stringResource(id = R.string.saved_streams_section_header_all),
                        style = MaterialTheme.typography.body1,
                        fontWeight = FontWeight.Normal,
                        color = fontColor(background),
                        textAlign = TextAlign.Left,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                }

                items(
                    items = uiState.recentStreams,
                    key = { it.streamName + it.accountID }
                ) { streamDetail ->

                    DismissibleRecentStream(
                        viewModel = viewModel,
                        streamDetail = streamDetail,
                        onClickAction = {
                            viewModel.add(streamDetail)
                            onPlayStream(streamingDataFrom(streamDetail))
                        }
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            if (showClearStreamsConfirmationDialog) {
                ClearStreamConfirmationAlert(
                    onClear = {
                        viewModel.clearAll()
                        showClearStreamsConfirmationDialog = false
                    },
                    onDismiss = {
                        showClearStreamsConfirmationDialog = false
                    },
                    modifier = modifier
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun DismissibleRecentStream(
    viewModel: SavedStreamViewModel,
    streamDetail: StreamDetail,
    onClickAction: (Context) -> Unit,
    modifier: Modifier = Modifier
) {
    val dismissState = rememberDismissState()
    if (dismissState.isDismissed(DismissDirection.EndToStart) && dismissState.currentValue != DismissValue.Default) {
        LaunchedEffect(Unit) {
            viewModel.delete(streamDetail)
            dismissState.snapTo(DismissValue.Default)
        }
    }
    SwipeToDismiss(
        state = dismissState,
        directions = setOf(DismissDirection.EndToStart),
        dismissThresholds = { _ ->
            FractionalThreshold(.75f)
        },
        background = {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(vertical = 5.dp)
                    .background(MaterialTheme.colors.error)
            ) {
                Image(
                    painterResource(id = io.dolby.uikit.R.drawable.ic_delete),
                    contentDescription = null,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 25.dp)
                )
            }
        }
    ) {
        StyledButton(
            buttonText = "${streamDetail.streamName} / ID ${streamDetail.accountID}",
            onClickAction = onClickAction,
            buttonType = ButtonType.BASIC,
            capitalize = false,
            modifier = modifier
        )
    }
}
