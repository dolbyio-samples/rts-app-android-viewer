package io.dolby.rtsviewer.uikit.input

import androidx.compose.foundation.layout.fillMaxWidth
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
        modifier = modifier.fillMaxWidth(),
        value = value,
        label = { Text(text = label, style = MaterialTheme.typography.body1) },
        onValueChange = onValueChange,
        enabled = enabled,
        readOnly = readOnly,
        colors = TextFieldDefaults.textFieldColors(MaterialTheme.colors.onBackground)
    )
}