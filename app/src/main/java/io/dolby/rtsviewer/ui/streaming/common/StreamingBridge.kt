package io.dolby.rtsviewer.ui.streaming.common

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

interface StreamingBridge {
    val streamStateInfos: StateFlow<List<StreamStateInfo>>
    fun populateStreamStateInfos(streamStates: List<StreamStateInfo>)
    fun updateSubscribedState(index: Int, isSubscribed: Boolean)
    fun hideSettings()
    fun updateShowSettings(index: Int, showSettings: Boolean)
    fun updateAvailableSteamingQualities(index: Int, availableStreamingQualities: List<AvailableStreamQuality>)
    fun updateSelectedQuality(selectedStreamQuality: AvailableStreamQuality)
    fun updateShowStatistics(show: Boolean)
}

class StreamingBridgeImpl : StreamingBridge {
    private val _streamStateInfos = MutableStateFlow<List<StreamStateInfo>>(emptyList())
    override val streamStateInfos: StateFlow<List<StreamStateInfo>> = _streamStateInfos.asStateFlow()

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

    override fun hideSettings() {
        val streamStateInfos = _streamStateInfos.value.map {
            it.copy(shouldShowSettings = false)
        }
        _streamStateInfos.update { streamStateInfos }
    }

    override fun updateShowSettings(index: Int, showSettings: Boolean) {
        val streamStateInfos = _streamStateInfos.value.map {
            if (it.streamInfo.index == index) {
                it.copy(shouldShowSettings = showSettings)
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
        val streamStateInfos = _streamStateInfos.value.map {
            if (it.shouldShowSettings) {
                it.copy(selectedStreamQuality = selectedStreamQuality)
            } else {
                it
            }
        }
        _streamStateInfos.update { streamStateInfos }
    }

    override fun updateShowStatistics(show: Boolean) {
        val streamStateInfos = _streamStateInfos.value.map {
            if (it.shouldShowSettings) {
                it.copy(showStatistics = show)
            } else {
                it
            }
        }
        _streamStateInfos.update { streamStateInfos }
    }
}
