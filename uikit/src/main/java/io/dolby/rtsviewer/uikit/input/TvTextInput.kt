package io.dolby.rtsviewer.uikit.input

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun TvTextInput(
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
    val isSelectedState = remember { mutableStateOf(false) }
    val isActivatedState = remember { mutableStateOf(false) }

    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val focusRequester = remember { FocusRequester() }
    val scope = rememberCoroutineScope()
    val textInputContentDescription =
        "${label.ifEmpty { value }} ${stringResource(id = textInputContentDescriptionId)}"

    if (isFocused) {
        LaunchedEffect(Unit) {
            delay(200)
            focusRequester.requestFocus()
        }
    }

    if (isSelectedState.value) {
        TextInput(
            modifier
                .focusRequester(focusRequester)
                .onFocusChanged {
                    if (!it.isFocused && isActivatedState.value) {
                        isActivatedState.value = false
                        isSelectedState.value = false
                    }
                }
                .semantics { contentDescription = textInputContentDescription }
                .testTag(textInputContentDescription),
            value,
            onValueChange,
            label,
            enabled,
            readOnly,
            keyboardOptions,
            keyboardActions,
            maximumCharacters
        )
    } else {
        TextInput(
            modifier
                .focusRequester(focusRequester)
                .clickable(
                    interactionSource = interactionSource,
                    indication = LocalIndication.current,
                    enabled = enabled && !readOnly,
                    onClick = {
                        scope.launch {
                            isSelectedState.value = true
                            delay(200)
                            focusRequester.requestFocus()
                        }
                    }
                )
                .onFocusChanged {
                    if (it.isFocused && isSelectedState.value) {
                        isActivatedState.value = true
                    }
                }
                .semantics { contentDescription = textInputContentDescription }
                .testTag(textInputContentDescription),
            value,
            onValueChange,
            label,
            enabled,
            true,
            keyboardOptions,
            keyboardActions,
            maximumCharacters
        )
    }
}
