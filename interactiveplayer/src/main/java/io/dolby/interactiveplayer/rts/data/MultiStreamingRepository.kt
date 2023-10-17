package io.dolby.interactiveplayer.rts.data

import android.util.Log
import com.millicast.AudioTrack
import com.millicast.LayerData
import com.millicast.Subscriber
import com.millicast.Subscriber.ProjectionData
import com.millicast.VideoTrack
import io.dolby.interactiveplayer.preferenceStore.AudioSelection
import io.dolby.interactiveplayer.preferenceStore.PrefsStore
import io.dolby.interactiveplayer.rts.data.MultiStreamingData.Companion.audio
import io.dolby.interactiveplayer.rts.data.MultiStreamingData.Companion.video
import io.dolby.interactiveplayer.rts.domain.MultiStreamStatisticsData
import io.dolby.interactiveplayer.rts.domain.StreamingData
import io.dolby.interactiveplayer.rts.utils.DispatcherProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch
import org.webrtc.RTCStatsReport
import java.lang.Exception
import java.util.Arrays
import java.util.Optional
import kotlin.jvm.optionals.getOrNull

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
        audioTrack: AudioTrack,
        id: String?,
        sourceId: String?
    ): MultiStreamingData {
        val audioTracks = audioTracks.toMutableList().apply {
            add(Audio(id, audioTrack, sourceId))
        }
        return copy(audioTracks = audioTracks)
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

class MultiStreamingRepository(
    private val prefsStore: PrefsStore,
    private val dispatcherProvider: DispatcherProvider
) {
    private val _data = MutableStateFlow(MultiStreamingData())
    val data: StateFlow<MultiStreamingData> = _data.asStateFlow()
    private var listener: Listener? = null

    private val _audioSelection = MutableStateFlow(AudioSelection.default)
    private var audioSelectionListenerJob: Job? = null

    init {
        listenForAudioSelection()
    }

    private fun listenForAudioSelection() {
        audioSelectionListenerJob?.cancel()
        audioSelectionListenerJob = CoroutineScope(dispatcherProvider.main).launch {
            prefsStore.audioSelection(data.value.streamingData).collect { audioSelection ->
                _audioSelection.update { audioSelection }
                when (audioSelection) {
                    AudioSelection.MainSource -> {
                        data.value.audioTracks.firstOrNull { it.sourceId == null }
                            ?.let { mainTrack ->
                                playAudio(mainTrack)
                            }
                    }

                    AudioSelection.FirstSource -> {
                        data.value.audioTracks.firstOrNull()?.let { firstTrack ->
                            playAudio(firstTrack)
                        }
                    }

                    AudioSelection.FollowVideo -> {
                        val selectedVideoTrack =
                            data.value.videoTracks.find { it.sourceId == data.value.selectedVideoTrackId }
                        val index = data.value.videoTracks.indexOf(selectedVideoTrack)
                        val audioTracks = data.value.audioTracks
                        if (index >= 0 && audioTracks.size > index) {
                            playAudio(audioTracks[index])
                        }
                    }

                    is AudioSelection.CustomAudioSelection -> {
                        val index =
                            data.value.videoTracks.indexOfFirst { it.sourceId == audioSelection.sourceId }
                        val audioTracks = data.value.audioTracks
                        if (index >= 0 && audioTracks.size > index) {
                            playAudio(audioTracks[index])
                        }
                    }
                }
            }
        }
    }

    fun connect(streamingData: StreamingData) {
        if (listener?.connected() == true) {
            return
        }
        val listener = Listener(_data, _audioSelection.asStateFlow())
        this.listener = listener
        val subscriber = Subscriber.createSubscriber(listener)

        val options = Subscriber.Option()
        options.statsDelayMs = 10_000
        options.disableAudio = false
        subscriber?.setOptions(options)
        subscriber.getStats(1_000)

        listener.subscriber = subscriber

        subscriber.credentials = credential(subscriber.credentials, streamingData)

        Log.d(TAG, "Connecting ...")

        try {
            subscriber.connect()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        _data.update { data -> data.copy(streamingData = streamingData) }
        listenForAudioSelection()
    }

    fun disconnect() {
        _data.update { MultiStreamingData() }
        listener?.disconnect()
        listenForAudioSelection()
    }

    private fun credential(
        credentials: Subscriber.Credential,
        streamingData: StreamingData
    ): Subscriber.Credential {
        credentials.streamName = streamingData.streamName
        credentials.accountId = streamingData.accountId
        credentials.apiUrl = "https://director.millicast.com/api/director/subscribe"
        return credentials
    }

    fun updateSelectedVideoTrackId(sourceId: String?) {
        _data.update { data ->
            val oldSelectedVideoTrackId = data.selectedVideoTrackId
            val oldSelectedVideoTrack =
                data.videoTracks.find { it.sourceId == oldSelectedVideoTrackId }
            oldSelectedVideoTrack?.videoTrack?.removeRenderer()
            val newSelectedVideoTrack = data.videoTracks.find { it.sourceId == sourceId }
            val index = data.videoTracks.indexOf(newSelectedVideoTrack)
            if (_audioSelection.value == AudioSelection.FollowVideo && index < data.audioTracks.size) {
                val newSelectedAudioTrack = data.audioTracks[index]
                listener?.playAudio(newSelectedAudioTrack)
            }
            data.copy(selectedVideoTrackId = sourceId)
        }
    }

    fun playVideo(
        video: MultiStreamingData.Video,
        preferredVideoQuality: VideoQuality,
        preferredVideoQualities: Map<String, VideoQuality>
    ) {
        val priorityVideoPreference =
            if (preferredVideoQuality != VideoQuality.AUTO) preferredVideoQualities[video.id] else null
        listener?.playVideo(video, priorityVideoPreference ?: preferredVideoQuality)
    }

    fun stopVideo(video: MultiStreamingData.Video) {
        listener?.stopVideo(video)
    }

    fun playAudio(audio: MultiStreamingData.Audio) {
        listener?.playAudio(audio)
    }

    private class Listener(
        private val data: MutableStateFlow<MultiStreamingData>,
        private val audioSelectionData: StateFlow<AudioSelection>
    ) : Subscriber.Listener {

        var subscriber: Subscriber? = null

        fun connected(): Boolean = subscriber?.isSubscribed ?: false

        fun disconnect() {
            subscriber?.disconnect()
            subscriber = null
        }

        override fun onConnected() {
            Log.d(TAG, "onConnected, this: $this, thread: ${Thread.currentThread().id}")
            val subscriber = subscriber ?: return

            val options = Subscriber.Option().apply {
                autoReconnect = true
            }

            subscriber.setOptions(options)
            subscriber.subscribe()
        }

        override fun onDisconnected() {
            Log.d(TAG, "onDisconnected")
            data.update {
                it.populateError(error = "Disconnected")
            }
        }

        override fun onConnectionError(p0: Int, p1: String?) {
            Log.d(TAG, "onConnectionError: $p0, $p1")
            data.update {
                it.populateError(error = p1 ?: "Unknown error")
            }
        }

        override fun onSignalingError(p0: String?) {
            Log.d(TAG, "onSignalingError: $p0")
            data.update {
                it.populateError(error = p0 ?: "Signaling error")
            }
        }

        override fun onStatsReport(p0: RTCStatsReport?) {
            p0?.let { report ->
                data.update { data ->
                    data.copy(
                        statisticsData = MultiStreamStatisticsData.from(
                            report
                        )
                    )
                }
            }
        }

        override fun onViewerCount(p0: Int) {
            Log.d(TAG, "onViewerCount: $p0")
            data.update { data -> data.copy(viewerCount = p0) }
        }

        override fun onSubscribed() {
            Log.d(TAG, "onSubscribed")
            val newData =
                data.updateAndGet { data -> data.copy(isSubscribed = true, error = null) }
            processPendingTracks(newData)
        }

        override fun onSubscribedError(p0: String?) {
            Log.d(TAG, "onSubscribedError: $p0")
            data.update {
                it.copy(error = p0 ?: "Subscribed error")
            }
        }

        override fun onTrack(p0: VideoTrack, p1: Optional<String>) {
            val mid = p1.getOrNull()
            Log.d(TAG, "onVideoTrack: $mid, $p0")
            data.update { data ->
                data.getPendingVideoTrackInfoOrNull()?.let { trackInfo ->
                    data.appendOtherVideoTrack(trackInfo, p0, mid, trackInfo.sourceId)
                } ?: data
            }
        }

        override fun onTrack(p0: AudioTrack, p1: Optional<String>) {
            Log.d(TAG, "onAudioTrack: ${p1.getOrNull()}, $p0")
            data.update { data ->
                data.getPendingAudioTrackInfoOrNull()?.let { trackInfo ->
                    data.appendAudioTrack(p0, p1.getOrNull(), trackInfo.sourceId)
                } ?: data
            }
            if (
                (
                    audioSelectionData.value == AudioSelection.FirstSource ||
                        audioSelectionData.value == AudioSelection.FollowVideo
                    ) &&
                data.value.audioTracks.size == 1
            ) {
                playAudio(data.value.audioTracks[0])
            }
            if (audioSelectionData.value is AudioSelection.CustomAudioSelection) {
                val selectionSourceId =
                    (audioSelectionData.value as AudioSelection.CustomAudioSelection).sourceId
                val index = data.value.videoTracks.indexOfFirst { it.sourceId == selectionSourceId }
                if (index >= 0 && data.value.audioTracks.size > index) {
                    playAudio(data.value.audioTracks[index])
                }
            }
        }

        override fun onFrameMetadata(p0: Int, p1: Int, p2: ByteArray?) {
            TODO("Not yet implemented")
        }

        override fun onActive(
            stream: String,
            tracksInfo: Array<out String>,
            optionalSourceId: Optional<String>
        ) {
            Log.d(TAG, "onActive: $stream, $tracksInfo, ${optionalSourceId.getOrNull()}")
            val sourceId = optionalSourceId.getOrNull()
            val pendingTracks = MultiStreamingData.parseTracksInfo(tracksInfo, sourceId)
            val newData = data.updateAndGet { data -> data.addPendingTracks(pendingTracks) }
            processPendingTracks(newData)
        }

        override fun onInactive(p0: String, p1: Optional<String>) {
            Log.d(TAG, "onInactive")
            val sourceId = p1.getOrNull()
            data.update { data ->
                data.videoTracks.filter { it.sourceId == sourceId }
                    .forEach { it.videoTrack.removeRenderer() }
                val inactiveVideo = data.videoTracks.firstOrNull { it.sourceId == sourceId }
                inactiveVideo?.let {
                    val tempVideos = data.videoTracks.toMutableList()
                    tempVideos.remove(inactiveVideo)
                    val selectedVideoTrack =
                        if (data.selectedVideoTrackId == sourceId) null else data.selectedVideoTrackId
                    return@update data.copy(
                        selectedVideoTrackId = selectedVideoTrack,
                        videoTracks = tempVideos
                    )
                }
                return@update data
            }
        }

        override fun onStopped() {
            Log.d(TAG, "onStopped")
        }

        override fun onVad(p0: String?, p1: Optional<String>?) {
            TODO("Not yet implemented")
        }

        override fun onLayers(
            mid: String?,
            activeLayers: Array<out LayerData>?,
            inactiveLayers: Array<out LayerData>?
        ) {
            Log.d(
                TAG,
                "onLayers: $mid, ${Arrays.toString(activeLayers)}, ${
                Arrays.toString(
                    inactiveLayers
                )
                }"
            )
            mid?.let {
                val filteredActiveLayers =
                    activeLayers?.filter { it.temporalLayerId == 0 || it.temporalLayerId == 0xff }
                val trackLayerDataList = when (filteredActiveLayers?.count()) {
                    2 -> listOf(
                        LowLevelVideoQuality.Auto(),
                        LowLevelVideoQuality.High(filteredActiveLayers[0]),
                        LowLevelVideoQuality.Low(filteredActiveLayers[1])
                    )

                    3 -> listOf(
                        LowLevelVideoQuality.Auto(),
                        LowLevelVideoQuality.High(filteredActiveLayers[0]),
                        LowLevelVideoQuality.Medium(filteredActiveLayers[1]),
                        LowLevelVideoQuality.Low(filteredActiveLayers[2])
                    )

                    else -> listOf(
                        LowLevelVideoQuality.Auto()
                    )
                }
                data.update {
                    val mutableOldTrackLayerData = it.trackLayerData.toMutableMap()
                    mutableOldTrackLayerData[mid] = trackLayerDataList
                    it.copy(trackLayerData = mutableOldTrackLayerData.toMap())
                }
            }
        }

        fun playVideo(
            video: MultiStreamingData.Video,
            preferredVideoQuality: VideoQuality
        ) {
            val availablePreferredVideoQuality =
                availablePreferredVideoQuality(video, preferredVideoQuality)
            val projected = data.value.trackProjectedData[video.id]
            if (projected == null || projected.videoQuality != availablePreferredVideoQuality?.videoQuality()) {
                Log.d(
                    TAG,
                    "project video ${video.id}, quality = $availablePreferredVideoQuality"
                )
                val projectionData = createProjectionData(video, availablePreferredVideoQuality)
                subscriber?.project(video.sourceId ?: "", arrayListOf(projectionData))
                data.update {
                    val mutableOldProjectedData = it.trackProjectedData.toMutableMap()
                    mutableOldProjectedData[projectionData.mid] = projectedDataFrom(
                        video.sourceId,
                        availablePreferredVideoQuality?.videoQuality() ?: VideoQuality.AUTO,
                        projectionData.mid
                    )
                    it.copy(trackProjectedData = mutableOldProjectedData.toMap())
                }
            }
        }

        private fun availablePreferredVideoQuality(
            video: MultiStreamingData.Video,
            preferredVideoQuality: VideoQuality
        ): LowLevelVideoQuality? {
            return data.value.trackLayerData[video.id]?.find {
                it.videoQuality() == preferredVideoQuality
            } ?: if (preferredVideoQuality != VideoQuality.AUTO) availablePreferredVideoQuality(
                video,
                preferredVideoQuality.lower()
            ) else {
                null
            }
        }

        fun stopVideo(video: MultiStreamingData.Video) {
            subscriber?.unproject(arrayListOf(video.id))
            data.update {
                val mutableOldProjectedData = it.trackProjectedData.toMutableMap()
                mutableOldProjectedData.remove(video.id)
                it.copy(trackProjectedData = mutableOldProjectedData.toMap())
            }
        }

        fun playAudio(audioTrack: MultiStreamingData.Audio) {
            val audioTrackIds = ArrayList(data.value.audioTracks.map { it.id })
            subscriber?.unproject(audioTrackIds)

            val projectionData = ProjectionData().also {
                it.mid = audioTrack.id
                it.trackId = audio
                it.media = audio
            }
            subscriber?.project(audioTrack.sourceId ?: "", arrayListOf(projectionData))
        }

        private fun processPendingTracks(data: MultiStreamingData) {
            if (data.isSubscribed) {
                val pendingTracks = data.pendingVideoTracks.count { !it.added }
                val pendingAudioTracks = data.pendingAudioTracks.count { !it.added }
                repeat(pendingTracks) {
                    subscriber?.addRemoteTrack(video)
                }
                repeat(pendingAudioTracks) {
                    subscriber?.addRemoteTrack(audio)
                }
                this.data.update { it.markPendingTracksAsAdded() }
            }
        }
    }

    companion object {
        private const val TAG = "io.dolby.interactiveplayer"

        fun createProjectionData(
            video: MultiStreamingData.Video,
            availablePreferredVideoQuality: LowLevelVideoQuality?
        ): ProjectionData = ProjectionData().also {
            it.mid = video.id
            it.trackId = video.trackId
            it.media = video.mediaType
            it.layer = availablePreferredVideoQuality?.layerData?.let { layerData ->
                Optional.of(layerData)
            }
        }

        fun projectedDataFrom(
            sourceId: String?,
            videoQuality: VideoQuality,
            mid: String
        ): MultiStreamingData.ProjectedData =
            MultiStreamingData.ProjectedData(
                mid = mid,
                sourceId = sourceId,
                videoQuality = videoQuality
            )
    }

    sealed class LowLevelVideoQuality(val layerData: LayerData?) {
        open fun videoQuality() = VideoQuality.AUTO
        class Auto : LowLevelVideoQuality(null)
        class Low(layerData: LayerData?) : LowLevelVideoQuality(layerData) {
            override fun videoQuality() = VideoQuality.LOW
        }

        class Medium(layerData: LayerData?) : LowLevelVideoQuality(layerData) {
            override fun videoQuality() = VideoQuality.MEDIUM
        }

        class High(layerData: LayerData?) : LowLevelVideoQuality(layerData) {
            override fun videoQuality() = VideoQuality.HIGH
        }
    }

    enum class VideoQuality {
        AUTO, LOW, MEDIUM, HIGH;

        fun lower(): VideoQuality {
            return when (this) {
                HIGH -> MEDIUM
                MEDIUM -> LOW
                LOW -> AUTO
                AUTO -> AUTO
            }
        }
    }
}

fun <T> List<T>.replace(newValue: T, block: (T) -> Boolean): List<T> {
    return map { if (block(it)) newValue else it }
}
