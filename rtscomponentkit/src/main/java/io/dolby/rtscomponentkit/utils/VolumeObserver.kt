package io.dolby.rtscomponentkit.utils

import android.content.Context
import android.database.ContentObserver
import android.media.AudioManager
import android.os.Handler
import com.millicast.AudioTrack

class VolumeObserver constructor(
    var context: Context,
    handler: Handler?,
    private val audioTrack: AudioTrack
) : ContentObserver(handler) {

    override fun onChange(selfChange: Boolean) {
        super.onChange(selfChange)
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val currentVolume: Int = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val maxVolume: Int = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        audioTrack.setVolume(currentVolume.toDouble() / maxVolume)
    }
}
