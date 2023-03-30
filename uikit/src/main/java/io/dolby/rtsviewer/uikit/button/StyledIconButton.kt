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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.dolby.rtsviewer.uikit.theme.fontColor
import io.dolby.uikit.R

@Composable
fun StyledIconButton(
    icon: Painter,
    modifier: Modifier = Modifier,
    text: String? = null,
    selected: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit = {}
) {
    val mutableInteractionSource = remember { MutableInteractionSource() }

    val tintColor = when {
        selected -> MaterialTheme.colors.onPrimary
        !enabled -> MaterialTheme.colors.onSecondary
        else -> MaterialTheme.colors.onBackground
    }
    val iconButtonContentDescription =
        "${text?.let { "$text " } ?: ""}${stringResource(id = R.string.iconButton_contentDescription)}"
    val iconContentDescription =
        "${text?.let { "$text " } ?: ""}${stringResource(id = R.string.icon_contentDescription)}"

    Row(
        modifier = modifier
            .clickable(
                interactionSource = mutableInteractionSource,
                indication = null,
                enabled = enabled,
                onClickLabel = text,
                role = Role.Button,
                onClick = onClick
            )
            .padding(17.dp)
            .semantics { contentDescription = iconButtonContentDescription }
    ) {
        text?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.body1,
                fontWeight = FontWeight.Medium,
                color = fontColor(MaterialTheme.colors.background),
                textAlign = TextAlign.End,
                modifier = Modifier
                    .align(CenterVertically)
                    .semantics { contentDescription = iconButtonContentDescription }
                    .padding(horizontal = 10.dp)
            )
        }
        Image(
            painter = icon,
            contentDescription = iconContentDescription,
            colorFilter = ColorFilter.tint(tintColor)
        )
    }
}
