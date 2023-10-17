package io.dolby.interactiveplayer.settings

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.dolby.interactiveplayer.R
import io.dolby.interactiveplayer.navigation.Screen
import io.dolby.interactiveplayer.preferenceStore.AudioSelection
import io.dolby.interactiveplayer.preferenceStore.MultiviewLayout
import io.dolby.interactiveplayer.preferenceStore.PrefsStore
import io.dolby.interactiveplayer.preferenceStore.StreamSortOrder
import io.dolby.interactiveplayer.rts.data.MultiStreamingData
import io.dolby.interactiveplayer.rts.data.MultiStreamingRepository
import io.dolby.interactiveplayer.rts.domain.StreamingData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val multiStreamingRepository: MultiStreamingRepository,
    private val preferencesStore: PrefsStore
) : ViewModel() {

    private val _audioTracks = MutableStateFlow<List<MultiStreamingData.Audio>>(emptyList())
    val videoTracks = _audioTracks.asStateFlow()

    private val _showSourceLabels = MutableStateFlow(false)
    val showSourceLabels = _showSourceLabels.asStateFlow()

    private val _multiviewLayout = MutableStateFlow(MultiviewLayout.default)
    val multiviewLayout = _multiviewLayout.asStateFlow()

    private val _streamSortOrder = MutableStateFlow(StreamSortOrder.default)
    val streamSortOrder = _streamSortOrder.asStateFlow()

    private val _audioSelection = MutableStateFlow(AudioSelection.default)
    val audioSelection = _audioSelection.asStateFlow()

    init {
        viewModelScope.launch {
            multiStreamingRepository.data.collect { data ->
                _audioTracks.update { data.audioTracks }
            }
        }
        viewModelScope.launch {
            preferencesStore.showSourceLabels(streamingData()).collect { enabled ->
                _showSourceLabels.update { enabled }
            }
        }
        viewModelScope.launch {
            preferencesStore.multiviewLayout(streamingData()).collect { layout ->
                _multiviewLayout.update { layout }
            }
        }
        viewModelScope.launch {
            preferencesStore.streamSourceOrder(streamingData()).collect { order ->
                _streamSortOrder.update { order }
            }
        }
        viewModelScope.launch {
            preferencesStore.audioSelection(streamingData()).collect { audioSelection ->
                _audioSelection.update { audioSelection }
            }
        }
    }

    private fun getStreamName(handle: SavedStateHandle): String? =
        handle[Screen.StreamSettings.ARG_STREAM_NAME]

    private fun getAccountId(handle: SavedStateHandle): String? =
        handle[Screen.StreamSettings.ARG_ACCOUNT_ID]

    fun streamingData(): StreamingData? =
        getStreamName(savedStateHandle)?.let { streamName ->
            getAccountId(savedStateHandle)?.let { accountId ->
                StreamingData(accountId, streamName)
            }
        }

    fun updateShowSourceLabels(show: Boolean) {
        viewModelScope.launch {
            preferencesStore.updateShowSourceLabels(show, streamingData())
        }
    }

    fun updateMultiviewLayout(layout: MultiviewLayout) {
        viewModelScope.launch {
            preferencesStore.updateMultiviewLayout(layout, streamingData())
        }
    }

    fun updateSortOrder(sortOrder: StreamSortOrder) {
        viewModelScope.launch {
            preferencesStore.updateStreamSourceOrder(sortOrder, streamingData())
        }
    }

    fun updateAudioSelection(audioSelection: AudioSelection) {
        viewModelScope.launch {
            preferencesStore.updateAudioSelection(audioSelection, streamingData())
        }
    }

    @StringRes
    fun footer(
        showMultiviewScreen: Boolean,
        showStreamSortOrderScreen: Boolean,
        showAudioSelectionScreen: Boolean
    ): Int? = when {
        showMultiviewScreen -> R.string.settings_multiview_layout_footer
        showStreamSortOrderScreen -> R.string.settings_stream_sort_order_footer
        showAudioSelectionScreen -> R.string.settings_audio_selection_footer
        else -> null
    }
}
