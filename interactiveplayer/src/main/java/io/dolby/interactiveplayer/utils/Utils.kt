package io.dolby.interactiveplayer.utils

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.media.AudioManager
import android.util.Log
import android.view.KeyEvent
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.nativeKeyCode
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.dolby.interactiveplayer.datastore.StreamDetail
import io.dolby.interactiveplayer.rts.domain.StreamingData
import java.io.File

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
fun isTVDeclarative(): Boolean {
    val context = LocalContext.current
    return context.packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)
}

fun isTV(context: Context): Boolean {
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

fun streamingDataFrom(streamDetail: StreamDetail): StreamingData {
    return StreamingData(
        accountId = streamDetail.accountID,
        streamName = streamDetail.streamName
    )
}

@Composable
fun horizontalPaddingDp(): Dp {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp
    return if (screenWidth <= 600) 15.dp else ((screenWidth - 450) / 2).dp
}

fun createDirectoryIfNotExists(directoryPath: String) {
    val directory = File(directoryPath)
    val tag = "io.dolby.interactiveplayer"
    if (!directory.exists()) {
        val isDirectoryCreated = directory.mkdirs()
        if (isDirectoryCreated) {
            Log.d(tag, "Directory was successfully created")
        } else {
            Log.d(tag, "Directory creation failed")
        }
    } else {
        Log.d(tag, "Directory already exists")
    }
}
