package io.dolby.interactiveplayer.rts.data

import android.content.Context
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Environment
import android.os.Handler
import android.os.HandlerThread
import android.provider.Settings
import android.util.Log
import com.millicast.Core
import com.millicast.devices.track.AudioTrack
import com.millicast.subscribers.Credential
import com.millicast.subscribers.Option
import io.dolby.interactiveplayer.detailInput.ConnectionOptions
import io.dolby.interactiveplayer.preferenceStore.AudioSelection
import io.dolby.interactiveplayer.preferenceStore.PrefsStore
import io.dolby.interactiveplayer.rts.domain.ConnectOptions
import io.dolby.interactiveplayer.rts.domain.MultiStreamingData
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
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

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
        audioManager.startBluetoothSco()
    }

    private fun turnSpeakerPhone() {
        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
        audioManager.stopBluetoothSco()
        audioManager.isBluetoothScoOn = false
        audioManager.isSpeakerphoneOn = true
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

    suspend fun connect(streamingData: StreamingData, connectOptions: ConnectOptions) {
        if (listener?.connected() == true) {
            return
        }
        val subscriber = Core.createSubscriber()

        var options = Option(
            statsDelayMs = 10_000,
            disableAudio = connectOptions.disableAudio,
            forcePlayoutDelay = connectOptions.forcePlayOutDelay,
            videoJitterMinimumDelayMs = connectOptions.videoJitterMinimumDelayMs
        )

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
            options = options.copy(rtcEventLogOutputPath = path + "/${timeStamp}_rtclogs.proto")
        }

        listener = Listener(_data, subscriber, options).apply {
            start()
        }
        subscriber.enableStats(true)

        subscriber.setCredentials(credential(subscriber.credentials, streamingData, connectOptions))

        Log.d(TAG, "Connecting ...")

        try {
            subscriber.connect(com.millicast.clients.ConnectionOptions(true))
        } catch (e: Exception) {
            e.printStackTrace()
        }
        _data.update { data -> data.copy(streamingData = streamingData) }
        listenForAudioSelection()
        listenForAudioDevices()
    }

    suspend fun disconnect() {
        val listener = listener
        this.listener = null
        listener?.disconnect()
        _data.update { MultiStreamingData() }
        unregisterVolumeObserver()
        unregisterAudioDeviceListener()
    }

    private fun credential(
        credentials: Credential,
        streamingData: StreamingData,
        connectOptions: ConnectOptions
    ) = credentials.copy(
        streamName = streamingData.streamName,
        accountId = streamingData.accountId,
        apiUrl = if (connectOptions.useDevEnv) {
            "https://director-dev.millicast.com/api/director/subscribe"
        } else {
            "https://director.millicast.com/api/director/subscribe"
        }
    )

    fun updateSelectedVideoTrackId(sourceId: String?) {
        _data.update { data ->
            val oldSelectedVideoTrackId = data.selectedVideoTrackId
            val oldSelectedVideoTrack =
                data.videoTracks.find { it.sourceId == oldSelectedVideoTrackId }
            oldSelectedVideoTrack?.videoTrack?.removeRenderer()

            data.copy(selectedVideoTrackId = sourceId)
        }
    }

    suspend fun playVideo(
        video: MultiStreamingData.Video,
        preferredVideoQuality: VideoQuality,
        preferredVideoQualities: Map<String, VideoQuality>
    ) {
        val priorityVideoPreference =
            if (preferredVideoQuality != VideoQuality.AUTO) preferredVideoQualities[video.id] else null
        listener?.playVideo(video, priorityVideoPreference ?: preferredVideoQuality)
    }

    suspend fun stopVideo(video: MultiStreamingData.Video) {
        listener?.stopVideo(video)
    }

    private suspend fun playAudio(audio: MultiStreamingData.Audio) {
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

    companion object {
        const val TAG = "io.dolby.interactiveplayer"
    }
}

fun <T> CoroutineScope.safeLaunch(
    block: suspend CoroutineScope.() -> T,
    error: (suspend CoroutineScope.(err: Throwable) -> T)? = null
) =
    this.launch {
        try {
            block(this)
        } catch (err: Throwable) {
            error?.invoke(this, err) ?: err.printStackTrace()
        }
    }
