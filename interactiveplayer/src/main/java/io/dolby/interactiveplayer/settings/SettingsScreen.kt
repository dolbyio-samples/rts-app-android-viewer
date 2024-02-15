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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Switch
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
import io.dolby.interactiveplayer.rts.ui.TopAppBar
import io.dolby.interactiveplayer.utils.horizontalPaddingDp
import io.dolby.rtscomponentkit.data.multistream.prefs.AudioSelection
import io.dolby.rtscomponentkit.data.multistream.prefs.MultiviewLayout
import io.dolby.rtscomponentkit.data.multistream.prefs.StreamSortOrder
import io.dolby.rtsviewer.uikit.text.Text
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

    val showDebugOptions = viewModel.showDebugOptions.collectAsState()
    val showSourceLabels = viewModel.showSourceLabels.collectAsState()
    val multiviewLayout = viewModel.multiviewLayout.collectAsState()
    val streamSortOrder = viewModel.streamSortOrder.collectAsState()
    val audioSelection = viewModel.audioSelection.collectAsState()

    val screenName = stringResource(
        id = when {
            showMultiviewScreen -> R.string.settings_multiview_layout
            showStreamSortOrderScreen -> R.string.settings_stream_sort_order
            showAudioSelectionScreen -> R.string.settings_audio_selection
            else -> viewModel.streamingData()?.let { R.string.stream_settings_screen_name }
                ?: R.string.global_settings_screen_name
        }
    )
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
                    showDebugOptions = showDebugOptions.value,
                    showSourceLabels = showSourceLabels.value,
                    multiviewLayout = multiviewLayout.value,
                    streamSortOrder = streamSortOrder.value,
                    audioSelection = audioSelection.value,
                    showMultiviewScreen = showMultiviewScreen,
                    showStreamSortOrderScreen = showStreamSortOrderScreen,
                    showAudioSelectionScreen = showAudioSelectionScreen
                )

                items(items = items) { selection: Selection ->
                    val onClick: (() -> Unit)? = selection.onClick ?: when (selection.nameRes) {
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
                            selection.nameRes?.let { stringResource(id = it) } ?: selection.name
                                ?: "",
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
                            Row(modifier = Modifier.align(Alignment.CenterVertically)) {
                                Text(
                                    text = stringResource(id = selection.currentValueRes),
                                    style = MaterialTheme.typography.body1,
                                    fontWeight = FontWeight.Medium,
                                    color = fontColor(background),
                                    textAlign = TextAlign.End
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    painter = painterResource(id = io.dolby.uikit.R.drawable.ic_arrow_right),
                                    contentDescription = stringResource(id = selection.nameRes!!)
                                )
                            }
                        } else if (selection.currentValue != null && selection.onClick == null) {
                            Row(modifier = Modifier.align(Alignment.CenterVertically)) {
                                Text(
                                    text = selection.currentValue,
                                    style = MaterialTheme.typography.body1,
                                    fontWeight = FontWeight.Medium,
                                    color = fontColor(background),
                                    textAlign = TextAlign.End
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    painter = painterResource(id = io.dolby.uikit.R.drawable.ic_arrow_right),
                                    contentDescription = stringResource(id = selection.nameRes!!)
                                )
                            }
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
    showDebugOptions: Boolean,
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
                null,
                null,
                multiviewLayout == MultiviewLayout.ListView
            ) {
                viewModel.updateMultiviewLayout(MultiviewLayout.ListView)
            },
            Selection(
                MultiviewLayout.SingleStreamView.stringResource(),
                null,
                null,
                null,
                multiviewLayout == MultiviewLayout.SingleStreamView
            ) {
                viewModel.updateMultiviewLayout(MultiviewLayout.SingleStreamView)
            },
            Selection(
                MultiviewLayout.GridView.stringResource(),
                null,
                null,
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
                null,
                null,
                streamSortOrder == StreamSortOrder.ConnectionOrder
            ) {
                viewModel.updateSortOrder(StreamSortOrder.ConnectionOrder)
            },
            Selection(
                StreamSortOrder.AlphaNumeric.stringResource(),
                null,
                null,
                null,
                streamSortOrder == StreamSortOrder.AlphaNumeric
            ) {
                viewModel.updateSortOrder(StreamSortOrder.AlphaNumeric)
            }
        )
    }

    showAudioSelectionScreen -> {
        val allAudioSelections = mutableListOf(
            Selection(
                AudioSelection.FirstSource.stringResource(),
                null,
                null,
                null,
                audioSelection == AudioSelection.FirstSource
            ) {
                viewModel.updateAudioSelection(AudioSelection.FirstSource)
            },
            Selection(
                AudioSelection.FollowVideo.stringResource(),
                null,
                null,
                null,
                audioSelection == AudioSelection.FollowVideo
            ) {
                viewModel.updateAudioSelection(AudioSelection.FollowVideo)
            }
        )

        viewModel.streamingData()?.let {
            viewModel.videoTracks.value.forEach {
                val isMainSource = it.sourceId == null
                allAudioSelections.add(
                    if (isMainSource) {
                        Selection(
                            AudioSelection.MainSource.stringResource(),
                            null,
                            null,
                            null,
                            audioSelection == AudioSelection.MainSource
                        ) {
                            viewModel.updateAudioSelection(AudioSelection.MainSource)
                        }
                    } else {
                        Selection(
                            null,
                            it.sourceId!!,
                            null,
                            null,
                            audioSelection is AudioSelection.CustomAudioSelection && audioSelection.sourceId == it.sourceId
                        ) {
                            viewModel.updateAudioSelection(AudioSelection.CustomAudioSelection(it.sourceId.toString()))
                        }
                    }
                )
            }
            allAudioSelections.toList()
        } ?: allAudioSelections.toList()
    }

    else -> {
        val commomSettings = mutableListOf(
            Selection(
                R.string.settings_show_source_labels,
                null,
                if (showSourceLabels) R.string.settings_value_true else R.string.settings_value_false,
                null,
                false
            ) {
                viewModel.updateShowSourceLabels(!showSourceLabels)
            },
            Selection(
                R.string.settings_multiview_layout,
                null,
                multiviewLayout.stringResource(),
                null,
                false,
                null
            ),
            Selection(
                R.string.settings_stream_sort_order,
                null,
                streamSortOrder.stringResource(),
                null,
                false,
                null
            ),
            Selection(
                R.string.settings_audio_selection,
                null,
                audioSelection.stringResource(),
                currentValue = if (audioSelection is AudioSelection.CustomAudioSelection) audioSelection.sourceId else null,
                false,
                null
            )
        )
        if (viewModel.streamingData() == null) {
            commomSettings.add(
                0,
                Selection(
                    R.string.settings_show_debug_options,
                    null,
                    if (showDebugOptions) R.string.settings_value_true else R.string.settings_value_false,
                    null,
                    false
                ) {
                    viewModel.updateShowDebugOptions(!showDebugOptions)
                }
            )
        }
        commomSettings.toList()
    }
}

class Selection(
    @StringRes val nameRes: Int?,
    val name: String?,
    val currentValueRes: Int?,
    val currentValue: String?,
    val selected: Boolean,
    val onClick: (() -> Unit)?
)
