package io.dolby.rtsviewer.ui.detailInput

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import io.dolby.rtscomponentkit.data.RTSViewerDataStore
import io.dolby.rtscomponentkit.utils.DispatcherProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DetailInputViewModel @Inject constructor(
    private val repository: RTSViewerDataStore,
    private val dispatcherProvider: DispatcherProvider
) : ViewModel() {
    private val defaultCoroutineScope = CoroutineScope(dispatcherProvider.default)

    fun connect(streamName: String, accountId: String) {
        defaultCoroutineScope.launch {
            repository.connect(streamName, accountId)
        }
    }
}