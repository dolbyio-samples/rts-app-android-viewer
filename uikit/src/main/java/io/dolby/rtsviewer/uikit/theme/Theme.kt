package com.dolby.uicomponents.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable

@Composable
fun RTSViewerTheme(content: @Composable () -> Unit) {
    val colors = getColorPalette().asList()

    MaterialTheme(
        colors = colors,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}

@Composable
fun getColorPalette(): ColorPalette {
    return if (isSystemInDarkTheme()) DarkThemeColors() else DarkThemeColors()//LightThemeColors()
}
