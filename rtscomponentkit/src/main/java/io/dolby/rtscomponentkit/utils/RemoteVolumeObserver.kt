package io.dolby.rtscomponentkit.utils

import android.content.Context
import android.database.ContentObserver
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import com.millicast.subscribers.remote.RemoteAudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class RemoteVolumeObserver(
    val context: Context,
    handler: Handler?,
    private val audioTrack: RemoteAudioTrack
) : ContentObserver(handler) {

    override fun onChange(selfChange: Boolean) {
        super.onChange(selfChange)
        CoroutineScope(DispatcherProviderImpl.default).launch {
            adjustTrackVolume(context, audioTrack)
        }
    }
}

suspend fun adjustTrackVolume(context: Context, audioTrack: RemoteAudioTrack) {
    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    val maxVolume: Int = audioManager.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL)
    val currentVolume: Int = audioManager.getStreamVolume(AudioManager.STREAM_VOICE_CALL)
    audioTrack.setVolume(currentVolume.toDouble() / maxVolume)
}
