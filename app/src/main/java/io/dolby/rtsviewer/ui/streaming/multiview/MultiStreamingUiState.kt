package io.dolby.rtsviewer.ui.streaming.multiview

import io.dolby.rtscomponentkit.data.MultiStreamStatisticsData
import io.dolby.rtscomponentkit.data.MultiStreamingData
import io.dolby.rtscomponentkit.data.MultiStreamingRepository

data class MultiStreamingUiState(
    val inProgress: Boolean = false,
    val accountId: String? = null,
    val streamName: String? = null,
    val videoTracks: List<MultiStreamingData.Video> = emptyList(),
    val audioTracks: List<MultiStreamingData.Audio> = emptyList(),
    val selectedVideoTrackId: String? = null,
    val error: io.dolby.rtsviewer.ui.streaming.Error? = null,
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
