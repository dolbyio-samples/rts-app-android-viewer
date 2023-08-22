package io.dolby.rtscomponentkit.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.toUpperCase
import androidx.compose.ui.unit.dp
import io.dolby.rtscomponentkit.R
import java.util.Locale

@Composable
fun LiveIndicator(modifier: Modifier, on: Boolean) {
    Box(
        modifier = modifier
            .padding(3.dp)
            .clip(shape = RoundedCornerShape(5.dp))
            .background(if (on) Color.Red else Color.Gray)
            .padding(horizontal = 15.dp, vertical = 5.dp)
    ) {
        Text(
            text = stringResource(
                id = if (on) R.string.live_indicator_on else R.string.live_indicator_off
            ).uppercase()
        )
    }
}