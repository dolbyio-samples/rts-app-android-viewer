package io.dolby.rtsviewer.uikit.button

import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalIndication
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
import androidx.compose.material.SnackbarDefaults.backgroundColor
import io.dolby.rtsviewer.uikit.text.Text
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
import androidx.compose.ui.unit.dp
import io.dolby.rtsviewer.uikit.theme.getColorPalette
import io.dolby.rtsviewer.uikit.theme.selectableButtonBackgroundColor
import io.dolby.rtsviewer.uikit.theme.selectableButtonBorderColor
import io.dolby.rtsviewer.uikit.theme.selectableButtonFontColor
import io.dolby.rtsviewer.uikit.utils.ViewState
import io.dolby.uikit.R

@Composable
fun StateButton(
    text: String,
    stateText: String,
    modifier: Modifier = Modifier,
    startIcon: Painter? = null,
    endIcon: Painter? = null,
    isEnabled: Boolean = true,
    onClick: () -> Unit = {}
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val isFocused by interactionSource.collectIsFocusedAsState()
    val viewState = ViewState.from(isPressed, false, isFocused, isEnabled)

    val backgroundColor = selectableButtonBackgroundColor(state = viewState)
    val borderColor = selectableButtonBorderColor(state = viewState)
    val primaryFontColor = selectableButtonFontColor(state = viewState, isPrimary = true)
    val secondaryFontColor = selectableButtonFontColor(state = viewState, isPrimary = false)

    val stateButtonContentDescription =
        "$text ${stringResource(id = R.string.stateButton_contentDescription)}"
    var stateButtonModifier = modifier
        .background(
            color = backgroundColor,
            shape = MaterialTheme.shapes.large
        )
        .border(
            width = 1.dp,
            color = borderColor,
            shape = MaterialTheme.shapes.large
        )
        .padding(horizontal = 15.dp, vertical = 12.dp)
        .semantics { contentDescription = stateButtonContentDescription }
        .testTag(stateButtonContentDescription)
    if (isEnabled) {
        stateButtonModifier = stateButtonModifier.clickable(
            interactionSource = interactionSource,
            indication = LocalIndication.current,
            role = Role.Button,
            onClick = onClick,
            enabled = true
        )
    }
    Row(
        modifier = stateButtonModifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val startIconContentDescription =
            stringResource(id = R.string.stateButton_startIcon_contentDescription)
        val endIconContentDescription =
            stringResource(id = R.string.stateButton_endIcon_contentDescription)
        startIcon?.let {
            Image(
                painter = it,
                contentDescription = startIconContentDescription,
                colorFilter = ColorFilter.tint(getColorPalette().neutralColor300)
            )
            Spacer(modifier = Modifier.width(12.dp))
        }
        Text(
            modifier = Modifier.weight(1f),
            text = text,
            color = primaryFontColor
        )
        Text(
            text = stateText,
            color = secondaryFontColor
        )
        Spacer(modifier = Modifier.width(7.dp))
        endIcon?.let {
            Image(
                modifier = Modifier.align(alignment = Alignment.CenterVertically),
                painter = endIcon,
                contentDescription = endIconContentDescription,
                colorFilter = ColorFilter.tint(secondaryFontColor)
            )
        }
    }
}
