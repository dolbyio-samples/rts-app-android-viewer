package io.dolby.interactiveplayer.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppViewModel @Inject constructor() : ViewModel() {
    private val _isPipEnabled = MutableStateFlow(false)
    val isPipEnabled = _isPipEnabled.asStateFlow()
    fun enablePip(enable: Boolean) {
        viewModelScope.launch {
            _isPipEnabled.emit(enable)
        }
    }
}
