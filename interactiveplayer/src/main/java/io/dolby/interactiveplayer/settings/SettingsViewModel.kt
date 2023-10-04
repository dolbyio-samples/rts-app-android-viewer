package io.dolby.interactiveplayer.settings

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.dolby.interactiveplayer.R
import io.dolby.interactiveplayer.preferenceStore.AudioSelection
import io.dolby.interactiveplayer.preferenceStore.MultiviewLayout
import io.dolby.interactiveplayer.preferenceStore.PrefsStore
import io.dolby.interactiveplayer.preferenceStore.StreamSortOrder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val globalPreferencesStore: PrefsStore
) : ViewModel() {

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
            globalPreferencesStore.showSourceLabels.collect { enabled ->
                _showSourceLabels.update { enabled }
            }
        }
        viewModelScope.launch {
            globalPreferencesStore.multiviewLayout.collect { layout ->
                _multiviewLayout.update { layout }
            }
        }
        viewModelScope.launch {
            globalPreferencesStore.streamSourceOrder.collect { order ->
                _streamSortOrder.update { order }
            }
        }
        viewModelScope.launch {
            globalPreferencesStore.audioSelection.collect { audioSelection ->
                _audioSelection.update { audioSelection }
            }
        }
    }

    fun updateShowSourceLabels(show: Boolean) {
        viewModelScope.launch {
            globalPreferencesStore.updateShowSourceLabels(show)
        }
    }

    fun updateMultiviewLayout(layout: MultiviewLayout) {
        viewModelScope.launch {
            globalPreferencesStore.updateMultiviewLayout(layout)
        }
    }

    fun updateSortOrder(sortOrder: StreamSortOrder) {
        viewModelScope.launch {
            globalPreferencesStore.updateStreamSourceOrder(sortOrder)
        }
    }

    fun updateAudioSelection(audioSelection: AudioSelection) {
        viewModelScope.launch {
            globalPreferencesStore.updateAudioSelection(audioSelection)
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
