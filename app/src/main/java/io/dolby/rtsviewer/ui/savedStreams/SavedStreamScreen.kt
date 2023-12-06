package io.dolby.rtsviewer.ui.savedStreams

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.material.MaterialTheme
import io.dolby.rtsviewer.uikit.text.Text
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.dolby.rtscomponentkit.ui.DolbyBackgroundBox
import io.dolby.rtsviewer.R
import io.dolby.rtsviewer.datastore.StreamDetail
import io.dolby.rtsviewer.uikit.button.ButtonType
import io.dolby.rtsviewer.uikit.button.StyledButton
import io.dolby.rtsviewer.uikit.theme.fontColor
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SavedStreamScreen(
    modifier: Modifier = Modifier,
    onPlayStream: (StreamDetail) -> Unit,
    viewModel: SavedStreamViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val focusRequester = remember { FocusRequester() }
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val scope = rememberCoroutineScope()
    var lastPlayedStreamCoordinates by remember {
        mutableStateOf(Rect.Zero)
    }

    LaunchedEffect(Unit) {
        delay(500)
        focusRequester.requestFocus()
    }

    DolbyBackgroundBox(
        showGradientBackground = false,
        modifier = modifier
    ) {
        val background = MaterialTheme.colors.background

        LazyColumn(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .width(450.dp)
                .align(Alignment.TopCenter)
                .padding(top = 16.dp, bottom = 16.dp)
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

                if (uiState.recentStreams.isNotEmpty()) {
                    val lastPlayedStream = uiState.recentStreams.first()

                    StyledButton(
                        buttonText = "${lastPlayedStream.streamName} / ID ${lastPlayedStream.accountID}",
                        onClickAction = {
                            onPlayStream(lastPlayedStream)
                        },
                        buttonType = ButtonType.BASIC,
                        capitalize = false,
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
                StyledButton(
                    buttonText = "${streamDetail.streamName} / ID ${streamDetail.accountID}",
                    onClickAction = {
                        onPlayStream(streamDetail)
                    },
                    buttonType = ButtonType.BASIC,
                    capitalize = false
                )

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}
