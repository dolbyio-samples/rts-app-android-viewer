package io.dolby.rtsviewer.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.dolby.rtsviewer.R
import io.dolby.rtsviewer.uikit.button.ButtonType
import io.dolby.rtsviewer.uikit.button.StyledButton
import io.dolby.rtsviewer.uikit.text.Text

@Composable
fun DetailInputConnectionAlert(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        modifier = modifier.semantics {
            contentDescription = "Connection Error"
        },
        onDismissRequest = onDismiss,
        text = {
            Text(text = stringResource(id = R.string.stream_offline_title_label))
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
