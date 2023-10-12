package io.dolby.interactiveplayer.detailInput

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.dolby.interactiveplayer.datastore.RecentStreamsDataStore
import io.dolby.interactiveplayer.rts.domain.StreamingData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DetailInputViewModel @Inject constructor(
    private val recentStreamsDataStore: RecentStreamsDataStore
) : ViewModel() {
    private val _uiState = MutableStateFlow(DetailInputScreenUiState())
    val uiState: StateFlow<DetailInputScreenUiState> = _uiState.asStateFlow()

    private val _streamName = MutableStateFlow("")
    var streamName = _streamName.asStateFlow()

    private val _accountId = MutableStateFlow("")
    var accountId = _accountId.asStateFlow()

    private var isDemo = false

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

    fun connect() {
        val streamingData =
            StreamingData(streamName = streamName.value, accountId = accountId.value)
        viewModelScope.launch {
            if (!isDemo) {
                // Save the stream detail
                recentStreamsDataStore.addStreamDetail(streamingData)
            }
        }
    }

    fun clearAllStreams() {
        viewModelScope.launch {
            recentStreamsDataStore.clearAll()
        }
    }

    val shouldPlayStream: Boolean
        get() = streamName.value.isNotEmpty() && accountId.value.isNotEmpty()

    fun updateStreamName(name: String) {
        _streamName.value = name
    }

    fun updateAccountId(id: String) {
        _accountId.value = id
    }

    fun useDemoStream() {
        isDemo = true
        _streamName.value = DEMO_STREAM_NAME
        _accountId.value = DEMO_ACCOUNT_ID
    }
}

const val DEMO_STREAM_NAME = "simulcastmultiview"
const val DEMO_ACCOUNT_ID = "k9Mwad"
