package io.dolby.rtsviewer.ui.savedStreams

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.dolby.rtscomponentkit.ui.DolbyBackgroundBox
import io.dolby.rtsviewer.R
import io.dolby.rtsviewer.datastore.StreamDetail
import io.dolby.rtsviewer.uikit.button.StyledButton
import io.dolby.rtsviewer.uikit.theme.fontColor

@Composable
fun SavedStreamScreen(
    modifier: Modifier = Modifier,
    onPlayStream: (StreamDetail) -> Unit,
    viewModel: SavedStreamViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    DolbyBackgroundBox(showGradientBackground = false, modifier = modifier) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = modifier
            .align(Alignment.Center)
            .fillMaxSize()
        ) {
            val background = MaterialTheme.colors.background

            Spacer(modifier = modifier.height(16.dp))

            Text(
                stringResource(id = R.string.saved_streams_screen_title),
                style = MaterialTheme.typography.h2,
                fontWeight = FontWeight.Bold,
                color = fontColor(background),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = modifier.height(8.dp))

            Column(
                horizontalAlignment = Alignment.Start,
                modifier = modifier
                    .verticalScroll(rememberScrollState())
                    .width(450.dp)
            ) {

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
                        buttonText = "${lastPlayedStream.streamName} / ${lastPlayedStream.accountID}",
                        onClickAction = {
                            onPlayStream(lastPlayedStream)
                        },
                        isPrimary = false
                    )
                }

                Spacer(modifier = modifier.height(16.dp))

                Text(
                    stringResource(id = R.string.saved_streams_section_header_all),
                    style = MaterialTheme.typography.body1,
                    fontWeight = FontWeight.Normal,
                    color = fontColor(background),
                    textAlign = TextAlign.Left
                )

                Spacer(modifier = modifier.height(8.dp))

                Column {
                    uiState.recentStreams.forEach { streamDetail ->
                        StyledButton(
                            buttonText = "${streamDetail.streamName} / ${streamDetail.accountID}",
                            onClickAction = {
                                onPlayStream(streamDetail)
                            },
                            isPrimary = false
                        )

                        Spacer(modifier = modifier.height(8.dp))
                    }
                }

                Spacer(modifier = modifier.height(16.dp))
            }
        }
    }
}