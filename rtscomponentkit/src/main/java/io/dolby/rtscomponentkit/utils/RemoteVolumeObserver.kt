package io.dolby.rtscomponentkit.utils

import android.content.Context
import android.database.ContentObserver
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import com.millicast.devices.track.AudioTrack
import com.millicast.subscribers.remote.RemoteAudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RemoteVolumeObserver(
    var context: Context,
    handler: Handler?,
    private val audioTrack: RemoteAudioTrack,
    private val dispatcherProvider: DispatcherProvider
) : ContentObserver(handler) {

    override fun onChange(selfChange: Boolean) {
        super.onChange(selfChange)
        CoroutineScope(dispatcherProvider.default).launch {
            adjustTrackVolume(context, audioTrack)
        }
    }
}

suspend fun adjustTrackVolume(context: Context, audioTrack: RemoteAudioTrack) {
    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    val maxVolume: Int = audioManager.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL)
    val minVolume: Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        audioManager.getStreamMinVolume(AudioManager.STREAM_VOICE_CALL)
    } else {
        0
    }
    val currentVolume: Int = audioManager.getStreamVolume(AudioManager.STREAM_VOICE_CALL)
    if (currentVolume - minVolume <= 0.1) {
        audioTrack.setVolume(0.0)
    } else {
        audioTrack.setVolume(currentVolume.toDouble() / maxVolume)
    }
}
