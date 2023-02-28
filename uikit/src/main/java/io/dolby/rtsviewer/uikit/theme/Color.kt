@file:Suppress("MemberVisibilityCanBePrivate")

package com.dolby.uicomponents.ui.theme

import androidx.compose.material.Colors
import androidx.compose.material.MaterialTheme
import androidx.compose.material.SwitchColors
import androidx.compose.material.SwitchDefaults
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import io.dolby.uikit.utils.ViewState

abstract class ColorPalette {
    open val blue = Color(0xFF3E44FE)
    open val blue20 = Color(0xFF6569FE)
    open val blue40 = Color(0xFF8B8FFE)
    open val blueDark20 = Color(0xFF3236CB)
    open val blueDark40 = Color(0xFF252998)

    open val white = Color(0xFFFFFFFF)
    open val blackDark = Color(0xFF14141A)

    open val lteBlue = Color(0xFF2AA0CC)
    open val purple = Color(0xFFAA33FF)
    open val pink = Color(0xFFFF2E7E)
    open val razzmatazz = Color(0xFFDA0059)
    open val green = Color(0xFF06B635)
    open val grayDark = Color(0xFF2c2c31)
    open val grayMedium = Color(0xFF6A6A6D)
    open val grayLight = Color(0xFFB9B9BA)
    open val darkGrayAp3 = Color(0xFF4A4A4A)

    open val grayDarkFont = Color(0xFF2C2C31)
    open val grayMediumFont = Color(0xFF6A6A66)
    open val grayLightFont = Color(0xFFD8D8D8)
    open val blueLightFont = Color(0xFF35C8FF)

    // Color's outside of Dolby palette
    open val transparent = Color.Transparent
    open val transparentGray = Color(0xAA6A6A66)
    open val black = Color.Black
    open val yellow = Color(0xFFFFFF00)
    open val red = Color(0xFFFF0000)



    abstract fun asList(): Colors
}

/*Light theme colour palette */
class LightThemeColors : ColorPalette() {
    override fun asList(): Colors {
        return lightColors(
            primary = blue,
            onPrimary = white,
            secondary = blueDark20,
            onSecondary = grayMediumFont,
            primaryVariant = blueDark40,
            secondaryVariant = blue20,
            background = white,
            onBackground = darkGrayAp3,
            surface = grayLight,
            onSurface = grayDarkFont,
            error = razzmatazz
        )
    }
}

class DarkThemeColors : ColorPalette() {
    override val lteBlue = Color(0xFF35C8FF)
    override val green = Color(0xFF59FFB4)

    override fun asList(): Colors {
        return darkColors(
            primary = blue,
            onPrimary = white,
            secondary = blue20,
            onSecondary = grayMediumFont,
            primaryVariant = purple,
            secondaryVariant = pink,
            background = blackDark,
            onBackground = white,
            surface = grayMedium,
            onSurface = grayLightFont,
            error = razzmatazz
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
internal fun borderColor(state: ViewState, isPrimary: Boolean): Color {
    return when (state) {
        ViewState.Selected,
        ViewState.Disabled,
        ViewState.Pressed -> backgroundColor(state, isPrimary)
        else -> MaterialTheme.colors.onBackground
    }
}

@Composable
internal fun backgroundColor(state: ViewState, isPrimary: Boolean): Color {
    return when (state) {
        ViewState.Disabled -> MaterialTheme.colors.surface
        ViewState.Pressed -> MaterialTheme.colors.secondary
        ViewState.Selected -> MaterialTheme.colors.primaryVariant
        else -> {
            if (isPrimary) {
                MaterialTheme.colors.primary
            } else {
                MaterialTheme.colors.background
            }
        }
    }
}

@Composable
fun fontColor(state: ViewState): Color {
    return when (state) {
        ViewState.Selected, ViewState.Disabled, ViewState.Pressed -> MaterialTheme.colors.onPrimary
        else -> MaterialTheme.colors.onBackground
    }
}
