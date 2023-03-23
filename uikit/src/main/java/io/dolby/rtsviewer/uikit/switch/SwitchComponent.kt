/*
 *
 *  This program is protected under international and U.S. copyright laws as
 *  an unpublished work. This program is confidential and proprietary to the
 *  copyright owners. Reproduction or disclosure, in whole or in part, or the
 *  production of derivative works therefrom without the express permission of
 *  the copyright owners is prohibited.
 *
 *                   Copyright (C) 2023 by Dolby Laboratories.
 *                              All rights reserved.
 *
 */

package io.dolby.rtsviewer.uikit.switch

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import io.dolby.rtsviewer.uikit.theme.fontColor
import io.dolby.rtsviewer.uikit.theme.selectableButtonBackgroundColor
import io.dolby.rtsviewer.uikit.theme.switchColours
import io.dolby.rtsviewer.uikit.utils.ViewState
import io.dolby.uikit.R

@Composable
fun SwitchComponent(
    text: String,
    checked: Boolean,
    isEnabled: Boolean,
    onCheckChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    icon: Painter? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val isFocused by interactionSource.collectIsFocusedAsState()
    val viewState = ViewState.from(isPressed, false, isFocused, isEnabled)

    val backgroundColor = selectableButtonBackgroundColor(state = viewState)
    val fontColor = if (isEnabled) fontColor(backgroundColor) else MaterialTheme.colors.onSurface

    Row(
        modifier = modifier
            .background(
                color = backgroundColor,
                shape = MaterialTheme.shapes.large
            )
            .border(
                width = 1.dp,
                color = fontColor,
                shape = MaterialTheme.shapes.large
            )
            .padding(horizontal = 15.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Switch,
                onClick = {
                    onCheckChange(!checked)
                }
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val switchContentDescription =
            "$text ${stringResource(id = R.string.switch_contentDescription)}"
        icon?.let {
            Image(
                modifier = Modifier.align(alignment = Alignment.CenterVertically),
                painter = icon,
                contentDescription = switchContentDescription,
                colorFilter = ColorFilter.tint(fontColor)
            )
            Spacer(modifier = Modifier.width(12.dp))
        }
        Text(
            modifier = Modifier
                .weight(1f)
                .align(alignment = Alignment.CenterVertically),
            text = text,
            color = fontColor
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckChange,
            enabled = isEnabled,
            colors = switchColours()
        )
    }
}
