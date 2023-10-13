package io.dolby.interactiveplayer.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import io.dolby.interactiveplayer.R
import io.dolby.interactiveplayer.preferenceStore.AudioSelection
import io.dolby.interactiveplayer.preferenceStore.MultiviewLayout
import io.dolby.interactiveplayer.preferenceStore.StreamSortOrder
import io.dolby.interactiveplayer.rts.ui.TopAppBar
import io.dolby.interactiveplayer.utils.horizontalPaddingDp
import io.dolby.rtsviewer.uikit.theme.fontColor

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    var showMultiviewScreen by remember { mutableStateOf(false) }
    var showStreamSortOrderScreen by remember { mutableStateOf(false) }
    var showAudioSelectionScreen by remember { mutableStateOf(false) }

    val background = MaterialTheme.colors.background
    val screenName =
        stringResource(
            id = viewModel.streamingData()?.let { R.string.stream_settings_screen_name }
                ?: R.string.global_settings_screen_name
        )

    val showSourceLabels = viewModel.showSourceLabels.collectAsState()
    val multiviewLayout = viewModel.multiviewLayout.collectAsState()
    val streamSortOrder = viewModel.streamSortOrder.collectAsState()
    val audioSelection = viewModel.audioSelection.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(screenName, onBack = {
                if (showMultiviewScreen || showStreamSortOrderScreen || showAudioSelectionScreen) {
                    showMultiviewScreen = false
                    showStreamSortOrderScreen = false
                    showAudioSelectionScreen = false
                } else {
                    onBack()
                }
            })
        },
        modifier = modifier
    ) { paddingValues ->
        Box(
            modifier = modifier
                .padding(paddingValues)
                .semantics { contentDescription = screenName }
        ) {
            LazyColumn(
                horizontalAlignment = Alignment.Start,
                modifier = modifier
                    .fillMaxWidth()
                    .align(Alignment.Center)
                    .clip(MaterialTheme.shapes.large)
                    .padding(horizontal = horizontalPaddingDp(), vertical = 16.dp)
            ) {
                val items = selectionItems(
                    viewModel = viewModel,
                    showSourceLabels = showSourceLabels.value,
                    multiviewLayout = multiviewLayout.value,
                    streamSortOrder = streamSortOrder.value,
                    audioSelection = audioSelection.value,
                    showMultiviewScreen = showMultiviewScreen,
                    showStreamSortOrderScreen = showStreamSortOrderScreen,
                    showAudioSelectionScreen = showAudioSelectionScreen
                )

                items(items = items) { selection: Selection ->
                    val onClick: (() -> Unit)? = selection.onClick ?: when (selection.name) {
                        R.string.settings_multiview_layout -> {
                            {
                                showMultiviewScreen = true
                            }
                        }

                        R.string.settings_stream_sort_order -> {
                            {
                                showStreamSortOrderScreen = true
                            }
                        }

                        R.string.settings_audio_selection -> {
                            {
                                showAudioSelectionScreen = true
                            }
                        }

                        else -> {
                            null
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp)
                            .clickable { onClick?.invoke() },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            stringResource(id = selection.name),
                            style = MaterialTheme.typography.body1,
                            fontWeight = FontWeight.Medium,
                            color = fontColor(background),
                            textAlign = TextAlign.Start
                        )
                        Spacer(
                            Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )

                        if (selection.currentValueRes == R.string.settings_value_true || selection.currentValueRes == R.string.settings_value_false) {
                            Switch(
                                checked = selection.currentValueRes == R.string.settings_value_true,
                                onCheckedChange = { onClick?.invoke() }
                            )
                        } else if (selection.currentValueRes != null && selection.onClick == null) {
                            Text(
                                text = stringResource(id = selection.currentValueRes),
                                style = MaterialTheme.typography.body1,
                                fontWeight = FontWeight.Medium,
                                color = fontColor(background),
                                textAlign = TextAlign.End
                            )
                        } else if (selection.selected) {
                            Image(
                                painter = painterResource(id = io.dolby.uikit.R.drawable.ic_selected),
                                contentDescription = "Selection",
                                modifier = Modifier
                                    .size(width = 15.dp, height = 15.dp)
                                    .align(Alignment.CenterVertically)
                            )
                        }
                    }
                }
                item {
                    val footerId = viewModel.footer(
                        showMultiviewScreen,
                        showStreamSortOrderScreen,
                        showAudioSelectionScreen
                    )
                    footerId?.let {
                        Text(
                            stringResource(id = footerId),
                            style = MaterialTheme.typography.caption,
                            fontWeight = FontWeight.Medium,
                            color = fontColor(background),
                            textAlign = TextAlign.Start
                        )
                    } ?: Spacer(modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}

fun selectionItems(
    viewModel: SettingsViewModel,
    showSourceLabels: Boolean,
    multiviewLayout: MultiviewLayout,
    streamSortOrder: StreamSortOrder,
    audioSelection: AudioSelection,
    showMultiviewScreen: Boolean,
    showStreamSortOrderScreen: Boolean,
    showAudioSelectionScreen: Boolean
): List<Selection> = when {
    showMultiviewScreen -> {
        listOf(
            Selection(
                MultiviewLayout.ListView.stringResource(),
                null,
                multiviewLayout == MultiviewLayout.ListView
            ) {
                viewModel.updateMultiviewLayout(MultiviewLayout.ListView)
            },
            Selection(
                MultiviewLayout.SingleStreamView.stringResource(),
                null,
                multiviewLayout == MultiviewLayout.SingleStreamView
            ) {
                viewModel.updateMultiviewLayout(MultiviewLayout.SingleStreamView)
            },
            Selection(
                MultiviewLayout.GridView.stringResource(),
                null,
                multiviewLayout == MultiviewLayout.GridView
            ) {
                viewModel.updateMultiviewLayout(MultiviewLayout.GridView)
            }
        )
    }

    showStreamSortOrderScreen -> {
        listOf(
            Selection(
                StreamSortOrder.ConnectionOrder.stringResource(),
                null,
                streamSortOrder == StreamSortOrder.ConnectionOrder
            ) {
                viewModel.updateSortOrder(StreamSortOrder.ConnectionOrder)
            },
            Selection(
                StreamSortOrder.AlphaNumeric.stringResource(),
                null,
                streamSortOrder == StreamSortOrder.AlphaNumeric
            ) {
                viewModel.updateSortOrder(StreamSortOrder.AlphaNumeric)
            }
        )
    }

    showAudioSelectionScreen -> {
        val allAudioSelections = listOf(
            Selection(
                AudioSelection.FirstSource.stringResource(),
                null,
                audioSelection == AudioSelection.FirstSource
            ) {
                viewModel.updateAudioSelection(AudioSelection.FirstSource)
            },
            Selection(
                AudioSelection.FollowVideo.stringResource(),
                null,
                audioSelection == AudioSelection.FollowVideo
            ) {
                viewModel.updateAudioSelection(AudioSelection.FollowVideo)
            },
            Selection(
                AudioSelection.MainSource.stringResource(),
                null,
                audioSelection == AudioSelection.MainSource
            ) {
                viewModel.updateAudioSelection(AudioSelection.MainSource)
            }
        )
        viewModel.streamingData()?.let {
            allAudioSelections
        } ?: allAudioSelections.subList(0, 2)
    }

    else -> {
        listOf(
            Selection(
                R.string.settings_show_source_labels,
                if (showSourceLabels) R.string.settings_value_true else R.string.settings_value_false,
                false
            ) {
                viewModel.updateShowSourceLabels(!showSourceLabels)
            },
            Selection(
                R.string.settings_multiview_layout,
                multiviewLayout.stringResource(),
                false,
                null
            ),
            Selection(
                R.string.settings_stream_sort_order,
                streamSortOrder.stringResource(),
                false,
                null
            ),
            Selection(
                R.string.settings_audio_selection,
                audioSelection.stringResource(),
                false,
                null
            )
        )
    }
}

class Selection(
    @StringRes val name: Int,
    val currentValueRes: Int?,
    val selected: Boolean,
    val onClick: (() -> Unit)?
)