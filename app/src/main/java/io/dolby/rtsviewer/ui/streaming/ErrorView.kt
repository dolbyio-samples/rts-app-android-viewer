package io.dolby.rtsviewer.ui.streaming

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun ErrorView(error: Error) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.background.copy(alpha = 0.75F))
    ) {
        Text(
            text = error.title,
            style = MaterialTheme.typography.h2,
            color = MaterialTheme.colors.onPrimary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        error.subtitle?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.body1,
                color = MaterialTheme.colors.onPrimary,
                textAlign = TextAlign.Center
            )
        }
    }
}
