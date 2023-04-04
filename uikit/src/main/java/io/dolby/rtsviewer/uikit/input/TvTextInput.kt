package io.dolby.rtsviewer.uikit.input

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
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
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    val isSelectedState = remember { mutableStateOf(false) }
    val isActivatedState = remember { mutableStateOf(false) }

    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val focusRequester = remember { FocusRequester() }
    val scope = rememberCoroutineScope()
    if (isFocused) {
        LaunchedEffect(Unit) {
            delay(200)
            focusRequester.requestFocus()
        }
    }

    Box(
        modifier = Modifier
            .clickable(
                interactionSource = interactionSource,
                indication = null,
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
                if (!it.isFocused && isActivatedState.value) {
                    isActivatedState.value = false
                    isSelectedState.value = false
                } else if (it.isFocused && isSelectedState.value) {
                    isActivatedState.value = true
                }
            }
    ) {
        if (isSelectedState.value) {
            TextInput(
                modifier.focusRequester(focusRequester),
                value,
                onValueChange,
                label,
                enabled,
                readOnly,
                keyboardOptions,
                keyboardActions
            )
        } else {
            TextInput(
                modifier.focusRequester(focusRequester),
                value,
                onValueChange,
                label,
                enabled,
                true,
                keyboardOptions,
                keyboardActions
            )
        }
    }
}
