@file:Suppress("MemberVisibilityCanBePrivate")

package io.dolby.rtsviewer.uikit.theme

import androidx.compose.material.Colors
import androidx.compose.material.MaterialTheme
import androidx.compose.material.SwitchColors
import androidx.compose.material.SwitchDefaults
import androidx.compose.material.darkColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import io.dolby.rtsviewer.uikit.utils.ViewState

abstract class ColorPalette {
    open val white = Color(0xFFFFFFFF)
    open val blackDark = Color(0xFF14141A)

    open val purple = Color(0xFFAA33FF)
    open val razzmatazz = Color(0xFFE52222)
    open val grayDark = Color(0xFF3D3D46)
    open val grayMedium = Color(0xFF34343B)
    open val grayLight = Color(0xFFB9B9BA)

    open val grayDarkFont = Color(0xFF2C2C31)
    open val grayMediumFont = Color(0xFF6A6A66)
    open val grayLightFont = Color(0xFFD8D8D8)

    // Color's outside of Dolby palette
    open val transparent = Color.Transparent
    open val transparentGray = Color(0xAA6A6A66)
    open val black = Color.Black
    open val yellow = Color(0xFFFFFF00)
    open val red = Color(0xFFFF0000)

    open val neutralColor25 = Color(0xFFFCFCFF)
    open val typographyTeritiary = Color(0xFF525259)

    abstract fun asList(): Colors
}

class DarkThemeColors : ColorPalette() {
    override fun asList(): Colors {
        return darkColors(
            primary = neutralColor25,
            primaryVariant = purple,
            onPrimary = white,
            secondary = blackDark,
            secondaryVariant = neutralColor25,
            onSecondary = white,
            background = blackDark,
            onBackground = white,
            surface = grayMedium,
            onSurface = grayLightFont,
            error = razzmatazz,
            onError = white
        )
    }
}

@Composable
fun switchColours(): SwitchColors {
    val colours = getColorPalette()
    return SwitchDefaults.colors(
        // From Figma, but I guess they'll change
        uncheckedTrackColor = colours.grayDark,
        checkedTrackColor = MaterialTheme.colors.primary,
        uncheckedThumbColor = colours.grayLight,
        checkedThumbColor = colours.grayLight
    )
}

@Composable
internal fun borderColor(state: ViewState, isPrimary: Boolean, isBasic: Boolean, isDanger: Boolean): Color {
    return if (isDanger) {
        backgroundColor(state, isPrimary = false, isBasic = false, isDanger = true)
    } else if (isBasic) {
        backgroundColor(state, isPrimary = false, isBasic = true, isDanger = false)
    } else if (isPrimary) {
        backgroundColor(state, isPrimary = true, isBasic = false, isDanger = false)
    } else {
        return when (state) {
            ViewState.Pressed,
            ViewState.Selected -> backgroundColor(state, false, isBasic = false, isDanger = false)
            ViewState.Disabled -> MaterialTheme.colors.surface
            ViewState.Focused -> MaterialTheme.colors.secondaryVariant
            ViewState.Unknown -> MaterialTheme.colors.primaryVariant
        }
    }
}

@Composable
internal fun backgroundColor(state: ViewState, isPrimary: Boolean, isBasic: Boolean, isDanger: Boolean): Color {
    return when (state) {
        ViewState.Disabled -> {
            if (isPrimary) MaterialTheme.colors.surface else MaterialTheme.colors.secondary
        }
        ViewState.Pressed, ViewState.Selected -> {
            if (isPrimary) MaterialTheme.colors.primary else MaterialTheme.colors.secondaryVariant
        }
        ViewState.Focused -> {
            if (isPrimary) {
                MaterialTheme.colors.primary
            } else {
                MaterialTheme.colors.secondaryVariant
            }
        }
        ViewState.Unknown -> {
            if (isDanger) {
                MaterialTheme.colors.error
            } else if (isBasic) {
                MaterialTheme.colors.surface
            } else if (isPrimary) {
                MaterialTheme.colors.primaryVariant
            } else {
                MaterialTheme.colors.secondary
            }
        }
    }
}

@Composable
fun fontColor(backgroundColor: Color): Color {
    return when (backgroundColor) {
        MaterialTheme.colors.primary -> DarkThemeColors().grayDarkFont
        MaterialTheme.colors.primaryVariant -> MaterialTheme.colors.onPrimary
        MaterialTheme.colors.secondary -> MaterialTheme.colors.onSecondary
        MaterialTheme.colors.secondaryVariant -> DarkThemeColors().grayDarkFont
        MaterialTheme.colors.background -> MaterialTheme.colors.onBackground
        MaterialTheme.colors.surface -> MaterialTheme.colors.onSurface
        MaterialTheme.colors.error -> MaterialTheme.colors.onError
        DarkThemeColors().grayMedium -> DarkThemeColors().typographyTeritiary
        else -> Color.Unspecified
    }
}
