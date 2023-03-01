package io.dolby.rtsviewer.ui.streaming

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import io.dolby.rtscomponentkit.data.RTSViewerDataStore
import io.dolby.rtscomponentkit.utils.DispatcherProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StreamingViewModel @Inject constructor(
    repository: RTSViewerDataStore,
    dispatcherProvider: DispatcherProvider
) : ViewModel() {
    private val defaultCoroutineScope = CoroutineScope(dispatcherProvider.default + Job())

    init {
        repository.connect()

        defaultCoroutineScope.launch {
            repository.state.collect {
                when (it) {
                    RTSViewerDataStore.State.Connected -> repository.startSubscribe()
                    RTSViewerDataStore.State.StreamInactive -> repository.stopSubscribe()

                    else -> {}
                }
            }
        }
    }
}