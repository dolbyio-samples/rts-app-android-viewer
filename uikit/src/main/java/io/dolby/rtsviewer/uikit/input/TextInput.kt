package io.dolby.rtsviewer.uikit.input

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.MaterialTheme
import androidx.compose.material.MaterialTheme.colors
import androidx.compose.material.OutlinedTextField
import io.dolby.rtsviewer.uikit.text.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import io.dolby.uikit.R

internal val textInputContentDescriptionId = R.string.textInput_contentDescription

@Preview(showBackground = true)
@Composable
fun TextInput(
    modifier: Modifier = Modifier,
    value: String = "",
    onValueChange: (String) -> Unit = {},
    label: String = "",
    enabled: Boolean = true,
    readOnly: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    maximumCharacters: Int? = null
) {
    val textInputContentDescription =
        "${label.ifEmpty { value }} ${stringResource(id = textInputContentDescriptionId)}"
    val text = remember { mutableStateOf(value) }

    OutlinedTextField(
        value = text.value,
        label = { Text(text = label, style = MaterialTheme.typography.body1) },
        onValueChange = {
            if (maximumCharacters == null || it.length <= maximumCharacters) {
                text.value = it
                onValueChange(it)
            }
        },
        shape = MaterialTheme.shapes.small,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        enabled = enabled,
        readOnly = readOnly,
        colors = TextFieldDefaults.textFieldColors(colors.onBackground),
        maxLines = 1,
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = textInputContentDescription }
            .testTag(textInputContentDescription)
    )
}
