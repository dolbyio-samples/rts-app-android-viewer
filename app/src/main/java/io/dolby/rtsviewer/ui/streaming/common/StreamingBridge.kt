package io.dolby.rtsviewer.ui.streaming.common

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

interface StreamingBridge {
    val streamStateInfos: StateFlow<List<StreamStateInfo>>
    val selectedStreamQuality: StateFlow<AvailableStreamQuality>
    val showStatistics: StateFlow<Boolean>
    fun populateStreamStateInfos(streamStates: List<StreamStateInfo>)
    fun updateSubscribedState(index: Int, isSubscribed: Boolean)
    fun updateAvailableSteamingQualities(index: Int, availableStreamingQualities: List<AvailableStreamQuality>)
    fun updateSelectedQuality(selectedStreamQuality: AvailableStreamQuality)
    fun updateShowStatistics(show: Boolean)
}

class StreamingBridgeImpl : StreamingBridge {
    private val _streamStateInfos = MutableStateFlow<List<StreamStateInfo>>(emptyList())
    override val streamStateInfos: StateFlow<List<StreamStateInfo>> = _streamStateInfos.asStateFlow()
    private val _selectedStreamQuality: MutableStateFlow<AvailableStreamQuality> = MutableStateFlow(AvailableStreamQuality.AUTO)
    override val selectedStreamQuality: StateFlow<AvailableStreamQuality> = _selectedStreamQuality
    private val _showStatistics = MutableStateFlow(false)
    override val showStatistics: StateFlow<Boolean> = _showStatistics.asStateFlow()

    override fun populateStreamStateInfos(streamStates: List<StreamStateInfo>) {
        _streamStateInfos.update { streamStates }
    }

    override fun updateSubscribedState(index: Int, isSubscribed: Boolean) {
        val streamStateInfos = _streamStateInfos.value.map {
            if (it.streamInfo.index == index) {
                it.copy(isSubscribed = isSubscribed)
            } else {
                it
            }
        }
        _streamStateInfos.update { streamStateInfos }
    }

    override fun updateAvailableSteamingQualities(index: Int, availableStreamingQualities: List<AvailableStreamQuality>) {
        val streamStateInfos = _streamStateInfos.value.map {
            if (it.streamInfo.index == index) {
                it.copy(availableStreamQualities = availableStreamingQualities)
            } else {
                it
            }
        }
        _streamStateInfos.update { streamStateInfos }
    }

    override fun updateSelectedQuality(selectedStreamQuality: AvailableStreamQuality) {
        _selectedStreamQuality.update { selectedStreamQuality }
    }

    override fun updateShowStatistics(show: Boolean) {
        _showStatistics.update { show }
    }
}
