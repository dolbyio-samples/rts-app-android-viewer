package io.dolby.rtsviewer.uikit.button

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
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import io.dolby.rtsviewer.uikit.theme.fontColor
import io.dolby.rtsviewer.uikit.theme.selectableButtonBackgroundColor
import io.dolby.rtsviewer.uikit.utils.ViewState
import io.dolby.uikit.R

@Composable
fun StateButton(
    text: String,
    stateText: String,
    isEnabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: Painter? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val isFocused by interactionSource.collectIsFocusedAsState()
    val viewState = ViewState.from(isPressed, false, isFocused, isEnabled)

    val backgroundColor = selectableButtonBackgroundColor(state = viewState)
    val borderColor = selectableButtonBackgroundColor(state = viewState)
    val fontColor = if (isEnabled) fontColor(backgroundColor) else MaterialTheme.colors.onSurface

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
            .padding(horizontal = 15.dp, vertical = 12.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Switch,
                onClick = onClick,
                enabled = isEnabled
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
        Text(
            modifier = Modifier
                .align(alignment = Alignment.CenterVertically),
            text = stateText,
            color = fontColor
        )
        Spacer(modifier = Modifier.width(7.dp))
        Image(
            modifier = Modifier.align(alignment = Alignment.CenterVertically),
            painter = painterResource(id = R.drawable.ic_arrow_right),
            contentDescription = switchContentDescription,
            colorFilter = ColorFilter.tint(fontColor)
        )
    }
}
