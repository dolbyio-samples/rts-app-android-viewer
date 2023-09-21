package io.dolby.rtsviewer.utils

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.media.AudioManager
import android.view.KeyEvent
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.nativeKeyCode
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalContext
import io.dolby.rtscomponentkit.data.RTSViewerDataStore
import io.dolby.rtscomponentkit.domain.StreamingData
import io.dolby.rtsviewer.R
import io.dolby.rtsviewer.datastore.StreamDetail

@Composable
fun KeepScreenOn(enabled: Boolean) {
    val context = LocalContext.current
    DisposableEffect(Unit) {
        val window = context.findActivity()?.window
        if (enabled) {
            window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
}

@Composable
fun SetupVolumeControlAudioStream() {
    val context = LocalContext.current
    context.findActivity()?.volumeControlStream = AudioManager.STREAM_MUSIC
}

fun Context.findActivity(): Activity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}

@Composable
fun isTV(): Boolean {
    val context = LocalContext.current
    return context.packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)
}

fun Modifier.anyDpadKeyEvent(action: () -> Unit): Modifier =
    this.onKeyEvent {
        if (it.key.nativeKeyCode != KeyEvent.KEYCODE_BACK) {
            action()
            return@onKeyEvent true
        }
        return@onKeyEvent false
    }

fun RTSViewerDataStore.StreamQualityType.titleResourceId(): Int {
    return when (this) {
        RTSViewerDataStore.StreamQualityType.Auto -> R.string.simulcast_auto
        is RTSViewerDataStore.StreamQualityType.High -> R.string.simulcast_high
        is RTSViewerDataStore.StreamQualityType.Medium -> R.string.simulcast_medium
        is RTSViewerDataStore.StreamQualityType.Low -> R.string.simulcast_low
    }
}

fun StreamingData.Companion.from(streamDetail: StreamDetail): StreamingData {
    return StreamingData(
        accountId = streamDetail.accountID,
        streamName = streamDetail.streamName,
        useDevEnv = streamDetail.useDevEnv,
        forcePlayOutDelay = streamDetail.forcePlayOutDelay,
        disableAudio = streamDetail.disableAudio,
        rtcLogs = streamDetail.rtcLogs,
        videoJitterMinimumDelayMs = streamDetail.videoJitterMinimumDelayMs
    )
}
