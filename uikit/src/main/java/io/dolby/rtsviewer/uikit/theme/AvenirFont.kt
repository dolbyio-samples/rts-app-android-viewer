/*
 *
 *  This program is protected under international and U.S. copyright laws as
 *  an unpublished work. This program is confidential and proprietary to the
 *  copyright owners. Reproduction or disclosure, in whole or in part, or the
 *  production of derivative works therefrom without the express permission of
 *  the copyright owners is prohibited.
 *
 *                   Copyright (C) 2022 by Dolby Laboratories.
 *                              All rights reserved.
 *
 */

package io.dolby.rtsviewer.uikit.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import io.dolby.uikit.R

// Define all the Avenir fonts
object AvenirFont {
    private val avenirBold =
        Font(
            resId = R.font.avenir_next_bold,
            weight = FontWeight.ExtraBold,
            style = FontStyle.Normal
        )
    private val avenirBoldItalic =
        Font(
            resId = R.font.avenir_next_bold_italic,
            weight = FontWeight.ExtraBold,
            style = FontStyle.Italic
        )
    private val avenirItalic =
        Font(
            resId = R.font.avenir_next_italic,
            weight = FontWeight.Normal,
            style = FontStyle.Italic
        )
    private val avenirMedium =
        Font(
            resId = R.font.avenir_next_medium,
            weight = FontWeight.Medium,
            style = FontStyle.Normal
        )
    private val avenirMediumItalic =
        Font(
            resId = R.font.avenir_next_medium_italic,
            weight = FontWeight.Medium,
            style = FontStyle.Italic
        )
    private val avenirRegular =
        Font(
            resId = R.font.avenir_next_regular,
            weight = FontWeight.Normal,
            style = FontStyle.Normal
        )
    private val avenirDemiBold =
        Font(
            resId = R.font.avenir_next_demi,
            weight = FontWeight.SemiBold,
            style = FontStyle.Normal
        )
    private val avenirDemiBoldItalic =
        Font(
            resId = R.font.avenir_next_demi_italic,
            weight = FontWeight.SemiBold,
            style = FontStyle.Italic
        )

    // Define the font family
    private val family = FontFamily(
        avenirBold,
        avenirBoldItalic,
        avenirItalic,
        avenirMedium,
        avenirMediumItalic,
        avenirRegular,
        avenirDemiBold,
        avenirDemiBoldItalic
    )

    // Factory creator methods
    fun with(
        fontSize: Int,
        fontStyle: FontStyle = FontStyle.Normal,
        fontWeight: FontWeight = FontWeight.Normal
    ): TextStyle = TextStyle(
        fontFamily = family,
        fontSize = fontSize.sp,
        fontStyle = fontStyle,
        fontWeight = fontWeight
    )

    fun mediumFontWith(
        fontSize: Int,
        fontStyle: FontStyle = FontStyle.Normal
    ): TextStyle = with(
        fontSize = fontSize,
        fontStyle = fontStyle,
        fontWeight = FontWeight.Medium
    )

    fun mediumItalicFontWith(
        fontSize: Int,
        fontStyle: FontStyle = FontStyle.Italic
    ): TextStyle = with(
        fontSize = fontSize,
        fontStyle = fontStyle,
        fontWeight = FontWeight.Medium
    )

    fun semiBoldFontWith(
        fontSize: Int,
        fontStyle: FontStyle = FontStyle.Normal
    ): TextStyle = with(
        fontSize = fontSize,
        fontStyle = fontStyle,
        fontWeight = FontWeight.SemiBold
    )

    fun boldFontWith(
        fontSize: Int,
        fontStyle: FontStyle = FontStyle.Normal
    ): TextStyle = with(
        fontSize = fontSize,
        fontStyle = fontStyle,
        fontWeight = FontWeight.Bold
    )
}
