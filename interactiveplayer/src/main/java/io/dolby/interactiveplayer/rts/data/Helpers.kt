package io.dolby.interactiveplayer.rts.data

import com.millicast.subscribers.ProjectionData
import io.dolby.interactiveplayer.rts.domain.MultiStreamingData

fun createProjectionData(
    video: MultiStreamingData.Video,
    availablePreferredVideoQuality: LowLevelVideoQuality?
) = ProjectionData(
    mid = video.id ?: "",
    trackId = video.trackId,
    media = video.mediaType,
    layer = availablePreferredVideoQuality?.layerData
)

fun projectedDataFrom(
    sourceId: String?,
    videoQuality: VideoQuality,
    mid: String
) = MultiStreamingData.ProjectedData(
    mid = mid,
    sourceId = sourceId,
    videoQuality = videoQuality
)
