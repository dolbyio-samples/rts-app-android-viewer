package io.dolby.rtsviewer.uikit.checkbox

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.Checkbox
import androidx.compose.material.MaterialTheme
import io.dolby.rtsviewer.uikit.text.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import io.dolby.rtsviewer.uikit.theme.checkboxColours
import io.dolby.rtsviewer.uikit.theme.selectableButtonBackgroundColor
import io.dolby.rtsviewer.uikit.theme.selectableButtonBorderColor
import io.dolby.rtsviewer.uikit.theme.selectableButtonFontColor
import io.dolby.rtsviewer.uikit.utils.ViewState
import io.dolby.uikit.R

@Composable
fun CheckBoxComponent(
    text: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val isFocused by interactionSource.collectIsFocusedAsState()
    val viewState = ViewState.from(isPressed, false, isFocused, isEnabled)

    val backgroundColor = selectableButtonBackgroundColor(state = viewState)
    val borderColor = selectableButtonBorderColor(state = viewState)
    val fontColor = selectableButtonFontColor(state = viewState, isPrimary = true)
    val checkBoxComponentContentDescription =
        "$text ${stringResource(id = R.string.checkbox_contentDescription)}"

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
                contentDescription = checkBoxComponentContentDescription
                toggleableState = if (checked) ToggleableState.On else ToggleableState.Off
            }
            .toggleable(
                value = checked,
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                enabled = isEnabled,
                role = Role.Checkbox,
                onValueChange = { onCheckedChange(!checked) }
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            modifier = Modifier.weight(1f),
            text = text,
            color = fontColor
        )
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = isEnabled,
            colors = checkboxColours(state = viewState)
        )
    }
}
