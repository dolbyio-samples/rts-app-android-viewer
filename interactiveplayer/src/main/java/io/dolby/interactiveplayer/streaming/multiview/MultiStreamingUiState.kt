package io.dolby.interactiveplayer.streaming.multiview

import com.millicast.subscribers.remote.RemoteAudioTrack
import com.millicast.subscribers.remote.RemoteVideoTrack
import io.dolby.rtscomponentkit.data.multistream.LowLevelVideoQuality
import io.dolby.rtscomponentkit.data.multistream.VideoQuality
import io.dolby.rtscomponentkit.domain.ConnectOptions
import io.dolby.rtscomponentkit.domain.MultiStreamStatisticsData

enum class Error {
    NO_INTERNET_CONNECTION, STREAM_NOT_ACTIVE
}
data class MultiStreamingUiState(
    val inProgress: Boolean = false,
    val accountId: String? = null,
    val streamName: String? = null,
    val connectOptions: ConnectOptions? = null,
    val videoTracks: List<RemoteVideoTrack> = emptyList(),
    val audioTracks: List<RemoteAudioTrack> = emptyList(),
    val selectedVideoTrackId: String? = null,
    val error: Error? = null,
    val hasNetwork: Boolean = true,
    val layerData: Map<String, List<LowLevelVideoQuality>> = emptyMap()
)

data class MultiStreamingStatisticsState(
    val showStatistics: Boolean = false,
    val statisticsData: MultiStreamStatisticsData? = null
)

data class MultiStreamingVideoQualityState(
    val showVideoQualitySelectionForMid: String? = null,
    val videoQualities: Map<String, VideoQuality> = emptyMap(),
    val availableVideoQualities: Map<String, List<LowLevelVideoQuality>> = emptyMap(),
    val preferredVideoQualities: Map<String, VideoQuality> = emptyMap()
)
