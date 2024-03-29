package io.dolby.interactiveplayer.detailInput

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import io.dolby.interactiveplayer.R
import io.dolby.rtsviewer.uikit.input.TextInput

const val MAXIMUM_CHARACTERS: Int = 64

@Composable
fun DetailInput(
    accountId: State<String>,
    streamName: State<String>,
    viewModel: DetailInputViewModel,
    localFocusManager: FocusManager,
    focusRequester: FocusRequester,
    playStream: () -> Unit
) {
    TextInput(
        value = streamName.value,
        label = stringResource(id = R.string.stream_name_placeholder),
        onValueChange = {
            viewModel.updateStreamName(it)
        },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        keyboardActions = KeyboardActions(
            onNext = { localFocusManager.moveFocus(FocusDirection.Down) }
        ),
        maximumCharacters = MAXIMUM_CHARACTERS,
        modifier = Modifier.focusRequester(focusRequester)
    )

    Spacer(modifier = Modifier.height(8.dp))

    TextInput(
        value = accountId.value,
        label = stringResource(id = R.string.account_id_placeholder),
        onValueChange = {
            viewModel.updateAccountId(it)
        },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        maximumCharacters = MAXIMUM_CHARACTERS
    )
}
