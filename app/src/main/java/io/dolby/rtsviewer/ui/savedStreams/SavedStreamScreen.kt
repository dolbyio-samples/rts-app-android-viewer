package io.dolby.rtsviewer.ui.savedStreams

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SavedStreamScreen(
    modifier: Modifier = Modifier,
    onPlayStream: (StreamDetail) -> Unit,
    viewModel: SavedStreamViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val focusRequester = remember { FocusRequester() }

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
            modifier = modifier
                .width(450.dp)
                .align(Alignment.TopCenter)
                .padding(top = 16.dp, bottom = 16.dp)
        ) {
            // Header - Title

            stickyHeader {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = modifier
                        .background(background)
                        .fillMaxWidth()
                ) {
                    Text(
                        stringResource(id = R.string.saved_streams_screen_title),
                        style = MaterialTheme.typography.h2,
                        fontWeight = FontWeight.Bold,
                        color = fontColor(background),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = modifier.height(8.dp))
                }
            }

            // Section - Last played stream

            item {
                Spacer(modifier = modifier.height(8.dp))

                Text(
                    stringResource(id = R.string.saved_streams_section_header_lastPlayed),
                    style = MaterialTheme.typography.body1,
                    fontWeight = FontWeight.Normal,
                    color = fontColor(background),
                    textAlign = TextAlign.Left
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
                        modifier = modifier
                            .focusRequester(focusRequester)
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
                    textAlign = TextAlign.Left
                )

                Spacer(modifier = modifier.height(8.dp))
            }

            items(items = uiState.recentStreams) { streamDetail ->
                StyledButton(
                    buttonText = "${streamDetail.streamName} / ID ${streamDetail.accountID}",
                    onClickAction = {
                        onPlayStream(streamDetail)
                    },
                    buttonType = ButtonType.BASIC,
                    capitalize = false
                )

                Spacer(modifier = modifier.height(8.dp))
            }
        }
    }
}
