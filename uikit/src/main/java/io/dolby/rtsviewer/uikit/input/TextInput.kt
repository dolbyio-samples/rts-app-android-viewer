package io.dolby.rtsviewer.uikit.input

import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun TextInput(
    value: String,
    label: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false
) {
    OutlinedTextField(
        modifier = modifier,
        value = value,
        label = { Text(text = label) },
        onValueChange = onValueChange,
        enabled = enabled,
        readOnly = readOnly,
        colors = TextFieldDefaults.textFieldColors(MaterialTheme.colors.onBackground)
    )
}