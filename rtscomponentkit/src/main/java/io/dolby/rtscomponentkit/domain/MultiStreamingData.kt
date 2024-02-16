package io.dolby.rtscomponentkit.domain

import com.millicast.devices.track.AudioTrack
import com.millicast.devices.track.VideoTrack
import io.dolby.rtscomponentkit.data.multistream.LowLevelVideoQuality
import io.dolby.rtscomponentkit.data.multistream.VideoQuality

data class MultiStreamingData(
    val videoTracks: List<Video> = emptyList(),
    val audioTracks: List<Audio> = emptyList(),
    val allAudioTrackIds: List<String?> = arrayListOf(),
    val selectedVideoTrackId: String? = null,
    val selectedAudioTrackId: String? = null,
    val viewerCount: Int = 0,
    val pendingMainAudioTrack: PendingMainAudioTrack? = null,
    val mainVideoTrackPendingTrack: PendingTrack? = null,
    val mainVideoTrackVideoTrack: MainVideoTrack? = null,
    val error: String? = null,
    val isSubscribed: Boolean = false,
    val streamingData: StreamingData? = null,
    val statisticsData: MultiStreamStatisticsData? = null,
    val trackLayerData: Map<String, List<LowLevelVideoQuality>> = emptyMap(),
    val trackProjectedData: Map<String, ProjectedData> = emptyMap()
) {
    data class Video(
        val id: String?,
        val videoTrack: VideoTrack,
        val sourceId: String?,
        val mediaType: String,
        val trackId: String,
        val active: Boolean
    )

    data class Audio(
        val id: String?,
        val audioTrack: AudioTrack,
        val sourceId: String?,
        val active: Boolean
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
        val videoQuality: VideoQuality
    )

    data class MainVideoTrack(
        val videoTrack: VideoTrack,
        val mid: String?
    )

    data class PendingMainAudioTrack(
        val audioTrack: AudioTrack,
        val mid: String?
    )

    internal fun appendOtherVideoTrack(
        pendingTrack: PendingTrack,
        videoTrack: VideoTrack,
        mid: String?,
        sourceId: String?
    ): MultiStreamingData {
        val videoTracks = videoTracks.toMutableList().apply {
            add(Video(mid, videoTrack, sourceId, pendingTrack.mediaType, pendingTrack.trackId, true))
        }
        return copy(videoTracks = videoTracks)
    }

    internal fun addPendingMainAudioTrack(
        audioTrack: AudioTrack,
        mid: String?
    ): MultiStreamingData {
        return copy(pendingMainAudioTrack = PendingMainAudioTrack(audioTrack, mid))
    }

    internal fun appendAudioTrack(
        audioTrack: AudioTrack,
        mid: String?,
        sourceId: String?
    ): MultiStreamingData {
        val audioTracks = audioTracks.toMutableList().apply {
            add(Audio(mid, audioTrack, sourceId, true))
        }
        val allAudioTracks = allAudioTrackIds.toMutableList().apply {
            add(mid)
        }
        return copy(
            audioTracks = audioTracks,
            allAudioTrackIds = allAudioTracks
        )
    }

    internal fun addMainVideoTrack(videoTrack: VideoTrack, mid: String?): MultiStreamingData =
        if (mainVideoTrackPendingTrack != null) {
            val videoTracks = videoTracks.toMutableList().apply {
                add(
                    Video(
                        mid,
                        videoTrack,
                        mainVideoTrackPendingTrack.sourceId,
                        mainVideoTrackPendingTrack.mediaType,
                        mainVideoTrackPendingTrack.trackId,
                        true
                    )
                )
            }
            copy(videoTracks = videoTracks, mainVideoTrackVideoTrack = MainVideoTrack(videoTrack, mid))
        } else {
            copy(mainVideoTrackVideoTrack = MainVideoTrack(videoTrack, mid))
        }

    internal fun addPendingMainAudioTrack(pendingTrack: PendingTrack?): MultiStreamingData {
        val pendingMainAudioTrack = pendingMainAudioTrack ?: return this
        pendingTrack ?: return this
        val audioTracks = audioTracks.toMutableList().apply {
            add(
                Audio(
                    pendingMainAudioTrack.mid,
                    pendingMainAudioTrack.audioTrack,
                    pendingTrack.sourceId,
                    true
                )
            )
        }
        val allAudioTracks = allAudioTrackIds.toMutableList().apply {
            add(pendingMainAudioTrack.mid)
        }
        return copy(audioTracks = audioTracks, allAudioTrackIds = allAudioTracks)
    }

    fun populateError(error: String): MultiStreamingData = copy(
        videoTracks = emptyList(),
        audioTracks = emptyList(),
        selectedAudioTrackId = null,
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
