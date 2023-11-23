package io.dolby.interactiveplayer.rts.data

import android.content.Context
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.HandlerThread
import android.provider.Settings
import android.util.Log
import com.millicast.AudioTrack
import com.millicast.LayerData
import com.millicast.Subscriber
import com.millicast.Subscriber.ProjectionData
import com.millicast.VideoTrack
import io.dolby.interactiveplayer.preferenceStore.AudioSelection
import io.dolby.interactiveplayer.preferenceStore.PrefsStore
import io.dolby.interactiveplayer.rts.domain.ConnectOptions
import io.dolby.interactiveplayer.rts.domain.MultiStreamStatisticsData
import io.dolby.interactiveplayer.rts.domain.MultiStreamingData
import io.dolby.interactiveplayer.rts.domain.MultiStreamingData.Companion.audio
import io.dolby.interactiveplayer.rts.domain.MultiStreamingData.Companion.video
import io.dolby.interactiveplayer.rts.domain.StreamingData
import io.dolby.interactiveplayer.rts.utils.DispatcherProvider
import io.dolby.interactiveplayer.utils.VolumeObserver
import io.dolby.interactiveplayer.utils.adjustTrackVolume
import io.dolby.interactiveplayer.utils.createDirectoryIfNotExists
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch
import org.webrtc.RTCStatsReport
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Arrays
import java.util.Optional
import kotlin.jvm.optionals.getOrNull

class MultiStreamingRepository(
    private val context: Context,
    private val prefsStore: PrefsStore,
    private val dispatcherProvider: DispatcherProvider
) {
    private val _data = MutableStateFlow(MultiStreamingData())
    val data: StateFlow<MultiStreamingData> = _data.asStateFlow()
    private var listener: Listener? = null

    private val _audioSelection = MutableStateFlow(AudioSelection.default)
    private var audioSelectionListenerJob: Job? = null

    private var volumeObserver: VolumeObserver? = null
    private val audioManager = context.getSystemService(AudioManager::class.java) as AudioManager
    private val handlerThread = HandlerThread("Audio Device Listener")
    private val audioDeviceCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>) {
            if (
                addedDevices.firstOrNull {
                    it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                        it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO
                } != null
            ) {
                turnBluetoothHeadset()
            }
        }

        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>) {
            if (removedDevices.firstOrNull {
                it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                    it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO
            } != null
            ) {
                turnSpeakerPhone()
            }
        }
    }

    init {
        listenForAudioSelection()
        handlerThread.start()
    }

    private fun listenForAudioDevices() {
        audioManager.registerAudioDeviceCallback(
            audioDeviceCallback,
            Handler(handlerThread.looper)
        )
        if (audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS).firstOrNull {
            it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO
        } != null
        ) {
            turnBluetoothHeadset()
        } else {
            turnSpeakerPhone()
        }
    }

    private fun unregisterAudioDeviceListener() {
        audioManager.unregisterAudioDeviceCallback(audioDeviceCallback)
    }

    private fun turnBluetoothHeadset() {
        audioManager.mode = AudioManager.MODE_IN_CALL
        audioManager.isBluetoothScoOn = true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val device = audioManager.availableCommunicationDevices.firstOrNull { it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO }
            device?.let {
                audioManager.setCommunicationDevice(it)
            }
        } else {
            audioManager.startBluetoothSco()
        }
    }

    private fun turnSpeakerPhone() {
        audioManager.mode = AudioManager.MODE_NORMAL
        audioManager.isBluetoothScoOn = false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val device = audioManager.availableCommunicationDevices.firstOrNull { it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER }
            device?.let {
                audioManager.clearCommunicationDevice()
                audioManager.setCommunicationDevice(it)
            }
        } else {
            audioManager.stopBluetoothSco()
            audioManager.isSpeakerphoneOn = true
        }
    }

    private fun listenForAudioSelection() {
        audioSelectionListenerJob?.cancel()
        audioSelectionListenerJob = CoroutineScope(dispatcherProvider.main).launch {
            combine(
                _data,
                prefsStore.audioSelection(data.value.streamingData)
            ) { data, audioSelection -> Pair(data, audioSelection) }.collect {
                val data = it.first
                val audioSelection = it.second
                var audioSourceIdToSelect: MultiStreamingData.Audio? = null
                _audioSelection.update { audioSelection }
                when (audioSelection) {
                    AudioSelection.MainSource -> {
                        data.audioTracks.firstOrNull { it.sourceId == null }
                            ?.let { mainTrack ->
                                audioSourceIdToSelect = mainTrack
                            }
                    }

                    AudioSelection.FirstSource -> {
                        data.audioTracks.firstOrNull()?.let { firstTrack ->
                            audioSourceIdToSelect = firstTrack
                        }
                    }

                    AudioSelection.FollowVideo -> {
                        val selectAudioTrack =
                            data.audioTracks.find { it.sourceId == data.selectedVideoTrackId }
                        selectAudioTrack?.let {
                            audioSourceIdToSelect = selectAudioTrack
                        }
                    }

                    is AudioSelection.CustomAudioSelection -> {
                        val audioTrack =
                            data.audioTracks.firstOrNull { it.sourceId == audioSelection.sourceId }
                        audioTrack?.let {
                            audioSourceIdToSelect = audioTrack
                        } ?: prefsStore.updateAudioSelection(
                            AudioSelection.default,
                            data.streamingData
                        )
                    }
                }
                audioSourceIdToSelect?.let { audio ->
                    if (audioSourceIdToSelect?.sourceId != data.selectedAudioTrackId) {
                        playAudio(audio)
                    }
                    adjustTrackVolume(context, audio.audioTrack)
                    addVolumeObserver(audio.audioTrack)
                }
            }
        }
    }

    fun connect(streamingData: StreamingData, connectOptions: ConnectOptions) {
        if (listener?.connected() == true) {
            return
        }
        val listener = Listener(_data)
        this.listener = listener
        val subscriber = Subscriber.createSubscriber(listener)

        val options = Subscriber.Option()
        options.autoReconnect = true
        options.statsDelayMs = 10_000
        options.disableAudio = connectOptions.disableAudio
        options.forcePlayoutDelay = connectOptions.forcePlayOutDelay
        options.videoJitterMinimumDelayMs = Optional.of(connectOptions.videoJitterMinimumDelayMs)
        if (connectOptions.rtcLogs) {
            val path = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS
            ).absolutePath + "/InteractivePlayer"
            createDirectoryIfNotExists(path)
            val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME.withZone(
                ZoneId.from(
                    ZoneOffset.UTC
                )
            )
            val timeStamp = formatter.format(Instant.now().truncatedTo(ChronoUnit.SECONDS))
                .replace(':', '-')
            options.rtcEventLogOutputPath = Optional.of(path + "/${timeStamp}_rtclogs.proto")
        }
        subscriber?.setOptions(options)
        subscriber.getStats(1_000)

        listener.subscriber = subscriber

        subscriber.credentials = credential(subscriber.credentials, streamingData, connectOptions)

        Log.d(TAG, "Connecting ...")

        try {
            subscriber.connect()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        _data.update { data -> data.copy(streamingData = streamingData) }
        listenForAudioSelection()
        listenForAudioDevices()
    }

    fun disconnect() {
        _data.update { MultiStreamingData() }
        unregisterVolumeObserver()
        listener?.disconnect()
        unregisterAudioDeviceListener()
    }

    private fun credential(
        credentials: Subscriber.Credential,
        streamingData: StreamingData,
        connectOptions: ConnectOptions
    ): Subscriber.Credential {
        credentials.streamName = streamingData.streamName
        credentials.accountId = streamingData.accountId
        if (connectOptions.useDevEnv) {
            credentials.apiUrl = "https://director-dev.millicast.com/api/director/subscribe"
        } else {
            credentials.apiUrl = "https://director.millicast.com/api/director/subscribe"
        }
        return credentials
    }

    fun updateSelectedVideoTrackId(sourceId: String?) {
        _data.update { data ->
            val oldSelectedVideoTrackId = data.selectedVideoTrackId
            val oldSelectedVideoTrack =
                data.videoTracks.find { it.sourceId == oldSelectedVideoTrackId }
            oldSelectedVideoTrack?.videoTrack?.removeRenderer()

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

    private fun playAudio(audio: MultiStreamingData.Audio) {
        listener?.playAudio(audio)
    }

    private fun addVolumeObserver(audioTrack: AudioTrack) {
        unregisterVolumeObserver()
        val volumeObserver = VolumeObserver(context, Handler(handlerThread.looper), audioTrack)
        context.contentResolver.registerContentObserver(
            Settings.System.CONTENT_URI,
            true,
            volumeObserver
        )
        this.volumeObserver = volumeObserver
    }

    private fun unregisterVolumeObserver() {
        volumeObserver?.let {
            context.contentResolver.unregisterContentObserver(it)
        }
        volumeObserver = null
    }

    private class Listener(
        private val data: MutableStateFlow<MultiStreamingData>
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
                if (data.videoTracks.isEmpty()) {
                    data.addPendingMainVideoTrack(p0, mid)
                } else {
                    data.getPendingVideoTrackInfoOrNull()?.let { trackInfo ->
                        data.appendOtherVideoTrack(trackInfo, p0, mid, trackInfo.sourceId)
                    } ?: data
                }
            }
        }

        override fun onTrack(p0: AudioTrack, p1: Optional<String>) {
            val mid = p1.getOrNull()
            Log.d(TAG, "onAudioTrack: $mid, $p0")
            data.update { data ->
                if (data.audioTracks.isEmpty()) {
                    data.addPendingMainAudioTrack(p0, mid)
                } else {
                    data.getPendingAudioTrackInfoOrNull()?.let { trackInfo ->
                        data.appendAudioTrack(trackInfo, p0, p1.getOrNull(), trackInfo.sourceId)
                    } ?: data
                }
            }
        }

        override fun onFrameMetadata(p0: Int, p1: Int, p2: ByteArray?) {
            Log.d(TAG, "onFrameMetadata: $p0, $p1")
            TODO("Not yet implemented")
        }

        override fun onActive(
            stream: String,
            tracksInfo: Array<out String>,
            optionalSourceId: Optional<String>
        ) {
            Log.d(TAG, "onActive: $stream, ${tracksInfo.toList()}, ${optionalSourceId.getOrNull()}")
            val sourceId = optionalSourceId.getOrNull()
            val pendingTracks = MultiStreamingData.parseTracksInfo(tracksInfo, sourceId)
            if (data.value.videoTracks.isEmpty()) {
                data.update { data ->
                    data.addPendingMainVideoTrack(pendingTracks.videoTracks.firstOrNull())
                }
            } else {
                val newData = data.updateAndGet { data -> data.addPendingTracks(pendingTracks, processAudio = false) }
                processPendingTracks(newData, processAudio = false)
            }
            if (data.value.audioTracks.isEmpty()) {
                data.update { data ->
                    data.addPendingMainAudioTrack(pendingTracks.audioTracks.firstOrNull())
                }
            } else {
                val newData = data.updateAndGet { data -> data.addPendingTracks(pendingTracks, processVideo = false) }
                processPendingTracks(newData, processVideo = false)
            }
        }

        override fun onInactive(p0: String, p1: Optional<String>) {
            Log.d(TAG, "onInactive $p0, ${p1.getOrNull()}")
            val sourceId = p1.getOrNull()
            data.update { data ->
                data.videoTracks.filter { it.sourceId == sourceId }
                    .forEach { it.videoTrack.removeRenderer() }
                val inactiveVideo = data.videoTracks.firstOrNull { it.sourceId == sourceId }
                var selectedVideoTrack = data.selectedVideoTrackId
                val tempVideos = data.videoTracks.toMutableList()
                inactiveVideo?.let {
                    tempVideos.remove(inactiveVideo)
                    if (data.selectedVideoTrackId == sourceId && tempVideos.isNotEmpty()) {
                        selectedVideoTrack = tempVideos[0].sourceId
                    }
                }
                val inactiveAudio = data.audioTracks.firstOrNull { it.sourceId == sourceId }
                val tempAudios = data.audioTracks.toMutableList()
                inactiveAudio?.let {
                    tempAudios.remove(inactiveAudio)
                }

                return@update data.copy(
                    selectedVideoTrackId = selectedVideoTrack,
                    videoTracks = tempVideos.toList(),
                    audioTracks = tempAudios.toList()
                )
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
            val audioTrackIds = ArrayList(data.value.allAudioTrackIds)
            subscriber?.unproject(audioTrackIds)

            val projectionData = ProjectionData().also {
                it.mid = audioTrack.id
                it.trackId = audio
                it.media = audio
            }
            subscriber?.project(audioTrack.sourceId ?: "", arrayListOf(projectionData))
            data.update {
                it.copy(selectedAudioTrackId = audioTrack.sourceId)
            }
        }

        private fun processPendingTracks(data: MultiStreamingData, processVideo: Boolean = true, processAudio: Boolean = true) {
            if (data.isSubscribed) {
                if (processVideo) {
                    val pendingVideoTracks = data.pendingVideoTracks.count { !it.added }
                    repeat(pendingVideoTracks) {
                        subscriber?.addRemoteTrack(video)
                    }
                }
                if (processAudio) {
                    val pendingAudioTracks = data.pendingAudioTracks.count { !it.added }
                    repeat(pendingAudioTracks) {
                        subscriber?.addRemoteTrack(audio)
                    }
                }
                this.data.update { it.markPendingTracksAsAdded(processVideo = processVideo, processAudio = processAudio) }
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

        companion object {
            fun valueToQuality(value: String): VideoQuality {
                return try {
                    VideoQuality.valueOf(value)
                } catch (ex: Exception) {
                    default
                }
            }

            val default: VideoQuality = AUTO
        }
    }
}

fun <T> List<T>.replace(newValue: T, block: (T) -> Boolean): List<T> {
    return map { if (block(it)) newValue else it }
}
