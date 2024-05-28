package io.dolby.rtscomponentkit.data.multistream

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
import com.millicast.Subscriber
import com.millicast.clients.ConnectionOptions
import com.millicast.subscribers.Credential
import com.millicast.subscribers.Option
import com.millicast.subscribers.remote.RemoteAudioTrack
import com.millicast.subscribers.state.SubscriberConnectionState
import io.dolby.rtscomponentkit.data.multistream.MultiStreamListener.Companion.TAG
import io.dolby.rtscomponentkit.data.multistream.prefs.AudioSelection
import io.dolby.rtscomponentkit.data.multistream.prefs.MultiStreamPrefsStore
import io.dolby.rtscomponentkit.domain.ConnectOptions
import io.dolby.rtscomponentkit.domain.MultiStreamingData
import io.dolby.rtscomponentkit.domain.StreamingData
import io.dolby.rtscomponentkit.utils.DispatcherProvider
import io.dolby.rtscomponentkit.utils.RemoteVolumeObserver
import io.dolby.rtscomponentkit.utils.adjustTrackVolume
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

class MultiStreamingRepository(
    private val context: Context,
    private val prefsStore: MultiStreamPrefsStore,
    private val dispatcherProvider: DispatcherProvider
) {
    private val _data = MutableStateFlow(MultiStreamingData())
    val data: StateFlow<MultiStreamingData> = _data.asStateFlow()
    private var listener: MultiStreamListener? = null

    private val _audioSelection = MutableStateFlow(AudioSelection.default)
    private var audioSelectionListenerJob: Job? = null
    private var connectionStateJob: Job? = null
    private var connectionJob: Job? = null
    private var subscriptionJob: Job? = null

    private var volumeObserver: RemoteVolumeObserver? = null
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

    private fun listenForConnectionState(subscriber: Subscriber, option: Option) {
        connectionStateJob?.cancel()
        connectionStateJob = CoroutineScope(dispatcherProvider.main).launch {
            subscriber.state.collect {
                when {
                    it.connectionState == SubscriberConnectionState.Connected -> {
                        if (!_data.value.isSubscribed && !_data.value.isSubscribing) {
                            _data.update {
                                it.copy(isSubscribing = true)
                            }
                            subscribe(subscriber, option)
                        }
                    }
                }
            }
        }
    }

    private fun listenForAudioSelection() {
        audioSelectionListenerJob?.cancel()
        audioSelectionListenerJob = CoroutineScope(dispatcherProvider.main).safeLaunch(block = {
            combine(
                data,
                prefsStore.audioSelection(data.value.streamingData)
            ) { data, audioSelection -> Pair(data, audioSelection) }.collect {
                val data = it.first
                val audioSelection = it.second
                var audioSourceIdToSelect: RemoteAudioTrack? = null
                _audioSelection.update { audioSelection }
                Log.d(TAG, "ListenForAudio Selection Audio audioTracks ${data.audioTracks}")
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
                        Log.d(TAG, "AudioListener followVideo $selectAudioTrack")
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
                    if (audio.sourceId != data.selectedAudioTrackId) {
                        Log.d(TAG, "Audio enable for source id ${audio.sourceId}")
                        data.audioTracks.firstOrNull { it.sourceId == data.selectedAudioTrackId }?.disableAsync()
                        audio.enableAsync()
                        updateSelectedAudioTrackId(audio.sourceId)
                        adjustTrackVolume(context, audio)
                        addVolumeObserver(audio)
                    }
                }
            }
        })
    }

    suspend fun connect(streamingData: StreamingData, connectOptions: ConnectOptions) {
        if (listener?.connected() == true) {
            return
        }
        val subscriber = Core.createSubscriber()
        listener = MultiStreamListener(_data, subscriber).apply {
            start()
        }
        subscriber.enableStats(true)

        subscriber.setCredentials(
            credential(
                subscriber.credentials,
                streamingData,
                connectOptions
            )
        )

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

        Log.d(TAG, "Connecting ...")

        connectionJob?.cancel()
        connectionJob = startConnection(subscriber, ConnectionOptions(true))
        _data.update { data -> data.copy(streamingData = streamingData) }
        listenForConnectionState(subscriber, options)
        listenForAudioSelection()
        listenForAudioDevices()
    }

    fun startConnection(subscriber: Subscriber, connectOptions: ConnectionOptions) =
        CoroutineScope(dispatcherProvider.io).launch {
            tryConnecting(subscriber, connectOptions, this)
        }

    suspend fun tryConnecting(
        subscriber: Subscriber,
        connectOptions: ConnectionOptions,
        coroutineScope: CoroutineScope
    ) {
        runCatching {
            Log.i(TAG, "Connect")
            subscriber.connect(connectOptions)
        }.onFailure {
            if (coroutineScope.isActive) {
                Log.i(TAG, "Connection failure with message ${it.message}")
                delay(2000)
                tryConnecting(subscriber, connectOptions, coroutineScope)
            }
        }
    }

    fun subscribe(subscriber: Subscriber, option: Option) {
        subscriptionJob?.cancel()
        subscriptionJob = CoroutineScope(dispatcherProvider.io).safeLaunch(block = {
            Log.d(TAG, "Start Subscribing ${option.disableAudio}")
            subscriber.subscribe(option)
        })
    }

    fun disconnect() {
        Log.d(TAG, "Disconnect")
        cancelAllJobs()
        listener?.disconnect()
        this.listener = null
        clearData()
        unregisterVolumeObserver()
        unregisterAudioDeviceListener()
    }

    private fun cancelAllJobs() {
        subscriptionJob?.cancel()
        connectionJob?.cancel()
        connectionStateJob?.cancel()
        audioSelectionListenerJob?.cancel()
        audioSelectionListenerJob = null
        subscriptionJob = null
        connectionJob = null
        connectionStateJob = null
    }

    private fun clearData() {
        _data.update { MultiStreamingData() }
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
            data.copy(selectedVideoTrackId = sourceId)
        }
    }

    fun updateSelectedAudioTrackId(sourceId: String?) {
        Log.d(TAG, "update SelectedAudio TrackId for sourceId $sourceId")
        _data.update { data ->
            data.copy(selectedAudioTrackId = sourceId)
        }
    }

    private fun addVolumeObserver(audioTrack: RemoteAudioTrack) {
        unregisterVolumeObserver()
        val volumeObserver = RemoteVolumeObserver(
            context,
            Handler(handlerThread.looper),
            audioTrack
        )
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
        const val TAG = "MultiStreamingRepository"
    }
}

fun createDirectoryIfNotExists(directoryPath: String) {
    val directory = File(directoryPath)
    if (!directory.exists()) {
        val isDirectoryCreated = directory.mkdirs()
        if (isDirectoryCreated) {
            Log.d(TAG, "Directory was successfully created")
        } else {
            Log.d(TAG, "Directory creation failed")
        }
    } else {
        Log.d(TAG, "Directory already exists")
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
