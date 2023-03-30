package io.dolby.rtsviewer.ui.alert

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.AlertDialog
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.dolby.rtsviewer.R
import io.dolby.rtsviewer.uikit.button.ButtonType
import io.dolby.rtsviewer.uikit.button.StyledButton

@Composable
fun ClearStreamConfirmationAlert(
    onClear: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        modifier = modifier.semantics {
            contentDescription = "Clear Stream Alert"
        },
        onDismissRequest = onDismiss,
        text = {
            Text(text = stringResource(id = R.string.delete_all_streams_dialog_label))
        },
        buttons = {
            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = modifier
                    .padding(all = 8.dp)
                    .fillMaxWidth()
            ) {
                StyledButton(
                    buttonText = stringResource(id = R.string.delete_all_streams_clear_button),
                    onClickAction = { onClear() },
                    buttonType = ButtonType.DANGER,
                    modifier = modifier
                        .width(200.dp)
                )

                Spacer(modifier = modifier.width(8.dp))

                StyledButton(
                    buttonText = stringResource(id = R.string.delete_all_streams_cancel_button),
                    onClickAction = {
                        onDismiss()
                    },
                    buttonType = ButtonType.SECONDARY,
                    modifier = modifier
                        .width(200.dp)
                )
            }
        }
    )
}
