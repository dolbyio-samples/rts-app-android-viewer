package io.dolby.interactiveplayer.rts.ui

import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import io.dolby.rtsviewer.uikit.text.Text

@Composable
fun TopAppBar(
    title: String,
    onBack: () -> Unit,
    actionIcon: Int? = null,
    onAction: (() -> Unit)? = null
) {
    androidx.compose.material.TopAppBar(title = { Text(text = title) }, navigationIcon = {
        IconButton(onClick = onBack) {
            Icon(Icons.Filled.ArrowBack, null)
        }
    }, actions = {
            onAction?.let {
                IconButton(onClick = onAction) {
                    Icon(painterResource(id = actionIcon ?: io.dolby.uikit.R.drawable.ic_settings), null)
                }
            }
        })
}
