package io.dolby.rtscomponentkit.ui

import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable

@Composable
fun TopAppBar(title: String, onBack: () -> Unit) {
    androidx.compose.material.TopAppBar(title = { Text(text = title)}, navigationIcon = {
        IconButton(onClick = onBack) {
            Icon(Icons.Filled.ArrowBack, null)
        }
    })
}