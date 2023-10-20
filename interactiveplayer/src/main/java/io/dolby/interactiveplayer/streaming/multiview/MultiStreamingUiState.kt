package io.dolby.interactiveplayer.streaming.multiview

import io.dolby.interactiveplayer.rts.data.MultiStreamingRepository
import io.dolby.interactiveplayer.rts.domain.MultiStreamStatisticsData
import io.dolby.interactiveplayer.rts.domain.MultiStreamingData

enum class Error {
    NO_INTERNET_CONNECTION, STREAM_NOT_ACTIVE
}
data class MultiStreamingUiState(
    val inProgress: Boolean = false,
    val accountId: String? = null,
    val streamName: String? = null,
    val videoTracks: List<MultiStreamingData.Video> = emptyList(),
    val audioTracks: List<MultiStreamingData.Audio> = emptyList(),
    val selectedVideoTrackId: String? = null,
    val error: Error? = null,
    val hasNetwork: Boolean = true,
    val layerData: Map<String, List<MultiStreamingRepository.LowLevelVideoQuality>> = emptyMap()
)

data class MultiStreamingStatisticsState(
    val showStatistics: Boolean = false,
    val statisticsData: MultiStreamStatisticsData? = null
)

data class MultiStreamingVideoQualityState(
    val showVideoQualitySelectionForMid: String? = null,
    val videoQualities: Map<String, MultiStreamingRepository.VideoQuality> = emptyMap(),
    val availableVideoQualities: Map<String, List<MultiStreamingRepository.LowLevelVideoQuality>> = emptyMap(),
    val preferredVideoQualities: Map<String, MultiStreamingRepository.VideoQuality> = emptyMap()
)
