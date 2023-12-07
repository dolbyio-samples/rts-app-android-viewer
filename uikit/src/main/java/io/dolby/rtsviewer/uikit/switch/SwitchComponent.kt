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
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import io.dolby.rtsviewer.uikit.text.Text
import io.dolby.rtsviewer.uikit.theme.getColorPalette
import io.dolby.rtsviewer.uikit.theme.selectableButtonBackgroundColor
import io.dolby.rtsviewer.uikit.theme.selectableButtonBorderColor
import io.dolby.rtsviewer.uikit.theme.selectableButtonFontColor
import io.dolby.rtsviewer.uikit.theme.switchColours
import io.dolby.rtsviewer.uikit.utils.ViewState
import io.dolby.uikit.R

@Composable
fun SwitchComponent(
    text: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true,
    startIcon: Painter? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val isFocused by interactionSource.collectIsFocusedAsState()
    val viewState = ViewState.from(isPressed, false, isFocused, isEnabled)

    val backgroundColor = selectableButtonBackgroundColor(state = viewState)
    val borderColor = selectableButtonBorderColor(state = viewState)
    val fontColor = selectableButtonFontColor(state = viewState, isPrimary = true)
    val switchComponentContentDescription = "$text ${stringResource(id = R.string.switch_contentDescription)}"

    Row(
        modifier = modifier
            .background(
                color = backgroundColor,
                shape = MaterialTheme.shapes.large
            )
            .border(
                width = 1.dp,
                color = borderColor,
                shape = MaterialTheme.shapes.large
            )
            .padding(horizontal = 15.dp)
            .semantics {
                contentDescription = switchComponentContentDescription
                toggleableState = if (checked) ToggleableState.On else ToggleableState.Off
            }
            .testTag(switchComponentContentDescription)
            .toggleable(
                value = checked,
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                enabled = isEnabled,
                role = Role.Switch,
                onValueChange = { onCheckedChange(!checked) }
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val switchContentDescription =
            "$text ${stringResource(id = R.string.switch_contentDescription)}"
        startIcon?.let {
            Image(
                painter = it,
                contentDescription = switchContentDescription,
                colorFilter = ColorFilter.tint(getColorPalette().neutralColor300)
            )
            Spacer(modifier = Modifier.width(12.dp))
        }
        Text(
            modifier = Modifier.weight(1f),
            text = text,
            color = fontColor
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = isEnabled,
            colors = switchColours(state = viewState)
        )
    }
}
