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

package io.dolby.rtsviewer.uikit.button

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.MaterialTheme.colors
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.dolby.rtsviewer.uikit.theme.backgroundColor
import io.dolby.rtsviewer.uikit.theme.borderColor
import io.dolby.rtsviewer.uikit.theme.fontColor
import io.dolby.rtsviewer.uikit.utils.ViewState
import io.dolby.rtsviewer.uikit.utils.buttonHeight
import io.dolby.uikit.R

internal val buttonContentDescriptionId = R.string.button_contentDescription

enum class ButtonType {
    PRIMARY, SECONDARY, DANGER, BASIC
}

@Preview
@Composable
fun StyledButton(
    modifier: Modifier = Modifier,
    buttonText: String = "",
    subtextTitle: String? = null,
    subtext: String? = null,
    onClickAction: ((Context) -> Unit)? = null,
    buttonType: ButtonType = ButtonType.PRIMARY,
    isSelected: Boolean = false,
    isEnabled: Boolean = true,
    isLarge: Boolean = false,
    capitalize: Boolean = true,
    endIcon: Painter? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val isFocused by interactionSource.collectIsFocusedAsState()

    val context = LocalContext.current
    val viewState = ViewState.from(isPressed, isSelected, isFocused, isEnabled)

    val backgroundColor = backgroundColor(state = viewState, buttonType = buttonType)
    val fontColor = if (isEnabled) fontColor(backgroundColor) else MaterialTheme.colors.onSurface
    val borderColor = borderColor(viewState, buttonType)
    val buttonContentDescription = "$buttonText ${stringResource(id = buttonContentDescriptionId)}"
    Button(
        interactionSource = interactionSource,
        onClick = { onClickAction?.invoke(context) },
        content = {
            if (buttonType == ButtonType.BASIC) {
                Row(
                    modifier = Modifier
                        .align(Alignment.CenterVertically)
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = if (capitalize) buttonText.uppercase() else buttonText,
                            style = MaterialTheme.typography.button,
                            textAlign = TextAlign.Start,
                            color = fontColor
                        )
                        subtext?.let {
                            Row {
                                subtextTitle?.let {
                                    Text(
                                        text = it,
                                        style = MaterialTheme.typography.button,
                                        textAlign = TextAlign.Start,
                                        color = fontColor,
                                        modifier = Modifier.padding(end = 5.dp)
                                    )
                                }
                                Text(
                                    text = if (capitalize) subtext.uppercase() else subtext,
                                    style = MaterialTheme.typography.body1,
                                    textAlign = TextAlign.Start,
                                    color = fontColor
                                )
                            }
                        }
                    }
                    endIcon?.let {
                        Image(
                            painter = endIcon,
                            contentDescription = "",
                            modifier = Modifier.align(Alignment.CenterVertically)
                        )
                    }
                }
            } else {
                Text(
                    text = if (capitalize) buttonText.uppercase() else buttonText,
                    style = MaterialTheme.typography.button,
                    textAlign = TextAlign.Center,
                    color = fontColor
                )
            }
        },
        colors = ButtonDefaults.textButtonColors(
            contentColor = fontColor,
            backgroundColor = backgroundColor
        ),
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, borderColor),
        enabled = isEnabled,
        modifier = modifier
            .buttonHeight(buttonType)
            .fillMaxWidth()
            .semantics { contentDescription = buttonContentDescription }
    )
}
