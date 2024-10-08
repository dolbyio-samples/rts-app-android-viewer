package io.dolby.rtscomponentkit.domain

import com.millicast.subscribers.remote.RemoteAudioTrack
import com.millicast.subscribers.remote.RemoteVideoTrack
import io.dolby.rtscomponentkit.data.multistream.LowLevelVideoQuality

data class MultiStreamingData(
    val videoTracks: List<RemoteVideoTrack> = emptyList(),
    val audioTracks: List<RemoteAudioTrack> = emptyList(),
    val allAudioTrackIds: List<String?> = arrayListOf(),
    val selectedVideoTrackId: String? = null,
    val selectedAudioTrackId: String? = "-1", // In order to differentiate between main source id which is null and initial state
    val viewerCount: Int = 0,
    val error: String? = null,
    val isSubscribed: Boolean = false,
    val isSubscribing: Boolean = false,
    val isConnected: Boolean = false,
    val streamingData: StreamingData? = null,
    val statisticsData: MultiStreamStatisticsData? = null,
    val trackLayerData: Map<String, List<LowLevelVideoQuality>> = emptyMap()
) {
    fun populateError(error: String): MultiStreamingData = copy(
        error = error,
        isConnected = false
    )

    companion object {
        const val video = "video"
        const val audio = "audio"
    }
}
