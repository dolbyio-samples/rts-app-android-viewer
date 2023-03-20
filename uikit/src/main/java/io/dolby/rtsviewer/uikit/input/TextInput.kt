package io.dolby.rtsviewer.uikit.input

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.TextFieldValue
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
    keyboardActions: KeyboardActions = KeyboardActions()
) {
    val textState = remember { mutableStateOf(TextFieldValue(value)) }
    val textInputContentDescription =
        "${label.ifEmpty { textState.value.text }} ${stringResource(id = textInputContentDescriptionId)}"
    OutlinedTextField(
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = textInputContentDescription },
        value = textState.value,
        label = { Text(text = label, style = MaterialTheme.typography.body1) },
        onValueChange = {
            onValueChange(it.text)
            textState.value = it
        },
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        enabled = enabled,
        readOnly = readOnly,
        colors = TextFieldDefaults.textFieldColors(MaterialTheme.colors.onBackground)
    )
}
