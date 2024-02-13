package io.dolby.interactiveplayer.rts.domain

import com.millicast.devices.track.AudioTrack
import com.millicast.devices.track.VideoTrack
import io.dolby.interactiveplayer.rts.data.LowLevelVideoQuality
import io.dolby.interactiveplayer.rts.data.VideoQuality

data class MultiStreamingData(
    val videoTracks: List<Video> = emptyList(),
    val audioTracks: List<Audio> = emptyList(),
    val allAudioTrackIds: List<String?> = arrayListOf(),
    val selectedVideoTrackId: String? = null,
    val selectedAudioTrackId: String? = null,
    val viewerCount: Int = 0,
    val pendingMainAudioTrack: PendingMainAudioTrack? = null,
    val pendingVideoTracks: List<PendingTrack> = emptyList(),
    val pendingAudioTracks: List<PendingTrack> = emptyList(),
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

    internal fun appendAudioTrack(
        pendingTrack: PendingTrack,
        audioTrack: AudioTrack,
        mid: String?,
        sourceId: String?
    ): MultiStreamingData {
        val pendingAudioTracks = pendingAudioTracks.toMutableList().apply { remove(pendingTrack) }
        val audioTracks = audioTracks.toMutableList().apply {
            add(Audio(mid, audioTrack, sourceId, true))
        }
        val allAudioTracks = allAudioTrackIds.toMutableList().apply {
            add(mid)
        }
        return copy(
            audioTracks = audioTracks,
            allAudioTrackIds = allAudioTracks,
            pendingAudioTracks = pendingAudioTracks
        )
    }

    private fun getPendingVideoTrackInfoOrNull(): PendingTrack? = pendingVideoTracks.firstOrNull()
    internal fun getPendingAudioTrackInfoOrNull(): PendingTrack? = pendingAudioTracks.firstOrNull()

    private fun appendOtherVideoTrack(
        pendingTrack: PendingTrack,
        videoTrack: VideoTrack,
        mid: String?,
        sourceId: String?
    ): MultiStreamingData {
        val pendingVideoTracks = pendingVideoTracks.toMutableList().apply { remove(pendingTrack) }

        val videoTracks = videoTracks.toMutableList().apply {
            add(Video(mid, videoTrack, sourceId, pendingTrack.mediaType, pendingTrack.trackId, true))
        }
        return copy(videoTracks = videoTracks, pendingVideoTracks = pendingVideoTracks)
    }

    internal fun addPendingTracks(
        pendingTracks: PendingTracks,
        processVideo: Boolean = true,
        processAudio: Boolean = true
    ): MultiStreamingData {
        val pendingVideoTracks = if (processVideo) {
            pendingVideoTracks.toMutableList().apply {
                addAll(pendingTracks.videoTracks)
            }
        } else pendingVideoTracks
        val pendingAudioTracks = if (processAudio) {
            pendingAudioTracks.toMutableList().apply {
                addAll(pendingTracks.audioTracks)
            }
        } else pendingAudioTracks
        return copy(
            pendingVideoTracks = pendingVideoTracks,
            pendingAudioTracks = pendingAudioTracks,
            error = null
        )
    }

    internal fun addPendingMainAudioTrack(
        audioTrack: AudioTrack,
        mid: String?
    ): MultiStreamingData {
        return copy(pendingMainAudioTrack = PendingMainAudioTrack(audioTrack, mid))
    }

    internal fun addVideoTrack(pendingTracks: List<PendingTrack>): MultiStreamingData = when {
        pendingTracks.isEmpty() -> this
        mainVideoTrackPendingTrack == null -> addMainVideoTrack(pendingTracks.first())
        else -> {
            val pendingVideoTracks = pendingVideoTracks.toMutableList().apply {
                addAll(pendingTracks)
            }
            copy(
                pendingVideoTracks = pendingVideoTracks,
                error = null
            )
        }
    }

    internal fun addVideoTrack(videoTrack: VideoTrack, mid: String?): MultiStreamingData {
        return if (mainVideoTrackVideoTrack == null) {
            addMainVideoTrack(videoTrack, mid)
        } else {
            getPendingVideoTrackInfoOrNull()?.let { trackInfo ->
                appendOtherVideoTrack(trackInfo, videoTrack, mid, trackInfo.sourceId)
            } ?: this
        }
    }

    private fun addMainVideoTrack(pendingTrack: PendingTrack): MultiStreamingData =
        if (mainVideoTrackVideoTrack != null) {
            val videoTracks = videoTracks.toMutableList().apply {
                add(
                    Video(
                        mainVideoTrackVideoTrack.mid,
                        mainVideoTrackVideoTrack.videoTrack,
                        pendingTrack.sourceId,
                        pendingTrack.mediaType,
                        pendingTrack.trackId,
                        true
                    )
                )
            }
            copy(videoTracks = videoTracks, mainVideoTrackPendingTrack = pendingTrack)
        } else {
            copy(mainVideoTrackPendingTrack = pendingTrack)
        }

    private fun addMainVideoTrack(videoTrack: VideoTrack, mid: String?): MultiStreamingData =
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

    fun markPendingTracksAsAdded(
        processVideo: Boolean = true,
        processAudio: Boolean = true
    ): MultiStreamingData {
        val pendingVideoTracks =
            if (processVideo) pendingVideoTracks.map { it.copy(added = true) } else pendingVideoTracks
        val pendingAudioTracks =
            if (processAudio) pendingAudioTracks.map { it.copy(added = true) } else pendingAudioTracks
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
