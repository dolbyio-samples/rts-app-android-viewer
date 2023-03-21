package io.dolby.rtsviewer.utils

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
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

fun Modifier.anyDpadKeyEvent(action: () -> Unit): Modifier =
    this.onKeyEvent {
        if (
            it.key.nativeKeyCode == KeyEvent.KEYCODE_DPAD_CENTER ||
            it.key.nativeKeyCode == KeyEvent.KEYCODE_DPAD_UP ||
            it.key.nativeKeyCode == KeyEvent.KEYCODE_DPAD_DOWN ||
            it.key.nativeKeyCode == KeyEvent.KEYCODE_DPAD_LEFT ||
            it.key.nativeKeyCode == KeyEvent.KEYCODE_DPAD_RIGHT ||
            it.key.nativeKeyCode == KeyEvent.KEYCODE_DPAD_UP_LEFT ||
            it.key.nativeKeyCode == KeyEvent.KEYCODE_DPAD_UP_RIGHT ||
            it.key.nativeKeyCode == KeyEvent.KEYCODE_DPAD_DOWN_LEFT ||
            it.key.nativeKeyCode == KeyEvent.KEYCODE_DPAD_DOWN_RIGHT
        ) {
            action()
            return@onKeyEvent true
        }
        return@onKeyEvent false
    }
