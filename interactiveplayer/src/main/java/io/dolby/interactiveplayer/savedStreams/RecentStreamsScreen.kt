package io.dolby.interactiveplayer.savedStreams

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.CircularProgressIndicator
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.dolby.interactiveplayer.R
import io.dolby.interactiveplayer.rts.domain.StreamingData
import io.dolby.interactiveplayer.rts.ui.DolbyCopyrightFooterView
import io.dolby.interactiveplayer.rts.ui.TopActionBar
import io.dolby.interactiveplayer.utils.streamingDataFrom
import io.dolby.rtsviewer.uikit.button.ButtonType
import io.dolby.rtsviewer.uikit.button.StyledButton
import io.dolby.rtsviewer.uikit.theme.fontColor
import kotlinx.coroutines.delay
import kotlin.math.min

private const val RECENTLY_VIEWED_COUNT_TO_DISPLAY = 3

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RecentStreamsScreen(
    modifier: Modifier = Modifier,
    onPlayNewClick: () -> Unit,
    onPlayStream: (StreamingData) -> Unit,
    onSavedStreamsClick: () -> Unit,
    onSettingsClick: () -> Unit,
    viewModel: SavedStreamViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val screenName = stringResource(id = R.string.recent_streams_screen_name)
    val background = MaterialTheme.colors.background
    var loading by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        delay(500)
        loading = false
    }

    Scaffold(
        topBar = {
            TopActionBar(onActionClick = onSettingsClick)
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
            if (loading) {
                Box(modifier = Modifier.fillMaxSize()) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
            } else if (uiState.recentStreams.isNotEmpty()) {
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
                        style = MaterialTheme.typography.h2,
                        fontWeight = FontWeight.Medium,
                        color = fontColor(background),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = modifier.height(8.dp))

                    Text(
                        stringResource(id = R.string.recently_viewed_subtitle),
                        style = MaterialTheme.typography.body2,
                        color = fontColor(background),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = modifier.height(12.dp))

                    Box(
                        modifier = modifier.fillMaxWidth()
                    ) {
                        Text(
                            stringResource(id = R.string.recent_streams),
                            style = MaterialTheme.typography.caption,
                            color = MaterialTheme.colors.onSurface,
                            modifier = Modifier.align(Alignment.CenterStart)
                        )
                        TextButton(
                            onClick = onSavedStreamsClick,
                            modifier = Modifier.align(Alignment.CenterEnd)
                        ) {
                            Text(
                                stringResource(id = R.string.recent_streams_view_all),
                                color = MaterialTheme.colors.onSurface
                            )
                        }
                    }

                    val amountToDisplay =
                        min(uiState.recentStreams.size, RECENTLY_VIEWED_COUNT_TO_DISPLAY)
                    if (amountToDisplay > 0) {
                        uiState.recentStreams.subList(0, amountToDisplay)
                            .forEach { streamDetail ->
                                StyledButton(
                                    buttonText = "${streamDetail.streamName} / ID ${streamDetail.accountID}",
                                    onClickAction = {
                                        viewModel.add(streamDetail)
                                        onPlayStream(streamingDataFrom(streamDetail))
                                    },
                                    buttonType = ButtonType.BASIC,
                                    capitalize = false
                                )
                            }
                    }

                    Spacer(modifier = modifier.height(12.dp))

                    Text(
                        stringResource(id = R.string.or_label),
                        style = MaterialTheme.typography.caption,
                        color = fontColor(background),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = modifier.height(12.dp))

                    StyledButton(
                        buttonText = stringResource(id = R.string.play_new_button),
                        onClickAction = {
                            onPlayNewClick()
                        },
                        buttonType = ButtonType.PRIMARY
                    )
                }
            } else {
                onPlayNewClick()
            }
        }
    }
}
