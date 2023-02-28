package com.dolby.uicomponents.ui.theme

import androidx.compose.material.Typography
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight

// Set of Material typography styles to start with
val Typography = Typography(

    // Use this as your default paragraph text

    h1 = AvenirFont.semiBoldFontWith(
        fontSize = 34,
    ),
    h2 = AvenirFont.semiBoldFontWith(
        fontSize = 28,
    ),
    h3 = AvenirFont.semiBoldFontWith(
        fontSize = 24,
    ),
    h4 = AvenirFont.semiBoldFontWith(
        fontSize = 22
    ),
    h5 = AvenirFont.semiBoldFontWith(
        fontSize = 20
    ),
    h6 = AvenirFont.boldFontWith(
        fontSize = 18
    ),
    subtitle1 = AvenirFont.mediumFontWith(
        fontSize = 14,
    ),
    subtitle2 = AvenirFont.mediumItalicFontWith(
        fontSize = 17,
    ),
    body1 = AvenirFont.mediumFontWith(
        fontSize = 16,
    ),
    body2 = AvenirFont.mediumFontWith(
        fontSize = 15
    ),
    caption = AvenirFont.with(
        fontSize = 12,
        fontWeight = FontWeight.Normal,
        fontStyle = FontStyle.Normal
    ),
    button = AvenirFont.with(
        fontWeight = FontWeight.Bold,
        fontSize = 15
    )
)
