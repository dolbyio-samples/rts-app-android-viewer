package io.dolby.interactiveplayer.rts.domain

import com.millicast.AudioTrack
import com.millicast.VideoTrack
import io.dolby.interactiveplayer.rts.data.MultiStreamingRepository

data class MultiStreamingData(
    val videoTracks: List<Video> = emptyList(),
    val audioTracks: List<Audio> = emptyList(),
    val selectedVideoTrackId: String? = null,
    val selectedAudioTrackId: String? = null,
    val viewerCount: Int = 0,
    val pendingVideoTracks: List<PendingTrack> = emptyList(),
    val pendingAudioTracks: List<PendingTrack> = emptyList(),
    val error: String? = null,
    val isSubscribed: Boolean = false,
    val streamingData: StreamingData? = null,
    val statisticsData: MultiStreamStatisticsData? = null,
    val trackLayerData: Map<String, List<MultiStreamingRepository.LowLevelVideoQuality>> = emptyMap(),
    val trackProjectedData: Map<String, ProjectedData> = emptyMap()
) {
    data class Video(
        val id: String?,
        val videoTrack: VideoTrack,
        val sourceId: String?,
        val mediaType: String,
        val trackId: String
    )

    data class Audio(
        val id: String?,
        val audioTrack: AudioTrack,
        val sourceId: String?
    )

    data class PendingTrack(
        val mediaType: String,
        val trackId: String,
        val sourceId: String?,
        val added: Boolean
    )

    internal class PendingTracks(
        val videoTracks: List<PendingTrack>,
        val audioTracks: List<PendingTrack>
    )

    data class ProjectedData(
        val mid: String,
        val sourceId: String?,
        val videoQuality: MultiStreamingRepository.VideoQuality
    )

    internal fun appendAudioTrack(
        pendingTrack: PendingTrack,
        audioTrack: AudioTrack,
        id: String?,
        sourceId: String?
    ): MultiStreamingData {
        val pendingAudioTracks = pendingAudioTracks.toMutableList().apply { remove(pendingTrack) }
        val audioTracks = audioTracks.toMutableList().apply {
            add(Audio(id, audioTrack, sourceId))
        }
        return copy(audioTracks = audioTracks, pendingAudioTracks = pendingAudioTracks)
    }

    internal fun getPendingVideoTrackInfoOrNull(): PendingTrack? = pendingVideoTracks.firstOrNull()
    internal fun getPendingAudioTrackInfoOrNull(): PendingTrack? = pendingAudioTracks.firstOrNull()

    internal fun appendOtherVideoTrack(
        pendingTrack: PendingTrack,
        videoTrack: VideoTrack,
        mid: String?,
        sourceId: String?
    ): MultiStreamingData {
        val pendingVideoTracks = pendingVideoTracks.toMutableList().apply { remove(pendingTrack) }

        val videoTracks = videoTracks.toMutableList().apply {
            add(Video(mid, videoTrack, sourceId, pendingTrack.mediaType, pendingTrack.trackId))
        }
        return copy(videoTracks = videoTracks, pendingVideoTracks = pendingVideoTracks)
    }

    internal fun addPendingTracks(pendingTracks: PendingTracks): MultiStreamingData {
        val pendingVideoTracks = pendingVideoTracks.toMutableList().apply {
            addAll(pendingTracks.videoTracks)
        }
        val pendingAudioTracks = pendingAudioTracks.toMutableList().apply {
            addAll(pendingTracks.audioTracks)
        }
        return copy(
            pendingVideoTracks = pendingVideoTracks,
            pendingAudioTracks = pendingAudioTracks,
            error = null
        )
    }

    fun markPendingTracksAsAdded(): MultiStreamingData {
        val pendingVideoTracks = pendingVideoTracks.map { it.copy(added = true) }
        val pendingAudioTracks = pendingAudioTracks.map { it.copy(added = true) }
        return copy(
            pendingVideoTracks = pendingVideoTracks,
            pendingAudioTracks = pendingAudioTracks,
            error = null,
            isSubscribed = true
        )
    }

    fun populateError(error: String): MultiStreamingData = copy(
        videoTracks = emptyList(),
        audioTracks = emptyList(),
        error = error,
        isSubscribed = false
    )

    companion object {
        const val video = "video"
        const val audio = "audio"

        internal fun parseTracksInfo(
            tracksInfo: Array<out String>,
            sourceId: String?
        ): PendingTracks {
            val videoTracks = mutableListOf<PendingTrack>()
            val audioTracks = mutableListOf<PendingTrack>()
            tracksInfo.forEach { trackInfo ->
                val trackInfoSplit = trackInfo.split("/")
                trackInfoSplit.firstOrNull()?.let { mediaType ->
                    when (mediaType) {
                        video -> videoTracks.add(
                            PendingTrack(
                                mediaType,
                                trackInfoSplit[1],
                                sourceId,
                                added = false
                            )
                        )

                        audio -> audioTracks.add(
                            PendingTrack(
                                mediaType,
                                trackInfoSplit[1],
                                sourceId,
                                added = false
                            )
                        )

                        else -> Unit
                    }
                }
            }
            return PendingTracks(videoTracks = videoTracks, audioTracks = audioTracks)
        }
    }
}