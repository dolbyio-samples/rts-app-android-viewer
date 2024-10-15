package io.dolby.rtsviewer.ui.streaming.common

import com.millicast.subscribers.state.LayerDataSelection
import io.dolby.rtsviewer.R

data class StreamInfo(
    val index: Int,
    val directorUrl: String,
    val streamName: String,
    val accountId: String
)

data class StreamStateInfo(
    val isSubscribed: Boolean = false,
    val shouldShowSettings: Boolean = false,
    val availableStreamQualities: List<AvailableStreamQuality> = emptyList(),
    val selectedStreamQuality: AvailableStreamQuality = AvailableStreamQuality.AUTO,
    val streamInfo: StreamInfo
)

sealed class StreamError(val titleResId: Int, val subtitleResId: Int? = null) {
    object NoInternetConnection : StreamError(R.string.stream_network_disconnected_label)
    object StreamNotActive : StreamError(R.string.stream_offline_title_label, R.string.stream_offline_subtitle_label)
}

sealed class AvailableStreamQuality(val titleResId: Int, val layerData: LayerDataSelection?) {
    object AUTO :
        AvailableStreamQuality(R.string.simulcast_auto, null)
    class High(layerData: LayerDataSelection) :
        AvailableStreamQuality(R.string.simulcast_high, layerData)
    class Medium(layerData: LayerDataSelection) :
        AvailableStreamQuality(R.string.simulcast_medium, layerData)

    class Low(layerData: LayerDataSelection) :
        AvailableStreamQuality(R.string.simulcast_low, layerData)
}
