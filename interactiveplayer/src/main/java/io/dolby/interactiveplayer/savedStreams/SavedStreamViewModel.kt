package io.dolby.interactiveplayer.savedStreams

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.dolby.interactiveplayer.datastore.RecentStreamsDataStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SavedStreamViewModel @Inject constructor(
    private val recentStreamsDataStore: RecentStreamsDataStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(SavedStreamScreenUiState())
    val uiState: StateFlow<SavedStreamScreenUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            recentStreamsDataStore.recentStreams
                .collectLatest {
                    _uiState.update { state ->
                        state.copy(
                            recentStreams = it
                        )
                    }
                }
        }
    }
    fun clearAll() {
        viewModelScope.launch {
            recentStreamsDataStore.clearAll()
        }
    }
}
