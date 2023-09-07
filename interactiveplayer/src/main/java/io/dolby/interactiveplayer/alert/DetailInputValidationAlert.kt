package io.dolby.interactiveplayer.alert

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.AlertDialog
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.dolby.interactiveplayer.R
import io.dolby.rtsviewer.uikit.button.ButtonType
import io.dolby.rtsviewer.uikit.button.StyledButton

@Composable
fun DetailInputValidationAlert(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        modifier = modifier.semantics {
            contentDescription = "Validation Alert"
        },
        onDismissRequest = onDismiss,
        text = {
            Text(text = stringResource(id = R.string.missing_stream_name_or_account_id))
        },
        buttons = {
            Column(
                modifier = modifier
                    .padding(all = 8.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                StyledButton(
                    buttonText = stringResource(id = R.string.missing_stream_detail_dismiss_button),
                    onClickAction = { onDismiss() },
                    buttonType = ButtonType.SECONDARY,
                    modifier = modifier
                        .width(200.dp)
                )
            }
        }
    )
}
