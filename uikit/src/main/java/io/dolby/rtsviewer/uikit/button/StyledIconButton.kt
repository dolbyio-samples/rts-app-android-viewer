/*
 * This program is protected under international and U.S. copyright laws as
 * an unpublished work. This program is confidential and proprietary to the
 * copyright owners. Reproduction or disclosure, in whole or in part, or the
 * production of derivative works therefrom without the express permission of
 * the copyright owners is prohibited.
 *
 *                 Copyright (C) 2023 by Dolby Laboratories.
 *                            All rights reserved.
 */

package io.dolby.rtsviewer.uikit.button

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.dolby.rtsviewer.uikit.theme.fontColor

@Composable
fun StyledIconButton(
    modifier: Modifier,
    contentDescription: String,
    iconRes: Int,
    text: String? = null,
    selected: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val tintColor = when {
        selected -> MaterialTheme.colors.onPrimary
        !enabled -> MaterialTheme.colors.onSecondary
        else -> MaterialTheme.colors.onBackground
    }
    Row {
        text?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.body1,
                fontWeight = FontWeight.Medium,
                color = fontColor(MaterialTheme.colors.background),
                textAlign = TextAlign.End,
                modifier = Modifier.align(CenterVertically)
            )
        }
        IconButton(
            modifier = modifier
                .padding(10.dp),
            enabled = enabled,
            onClick = onClick
        ) {
            Image(
                painter = painterResource(iconRes),
                contentDescription = contentDescription,
                colorFilter = ColorFilter.tint(tintColor)
            )
        }
    }
}
