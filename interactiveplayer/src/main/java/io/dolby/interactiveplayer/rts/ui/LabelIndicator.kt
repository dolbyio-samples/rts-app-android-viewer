package io.dolby.interactiveplayer.rts.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.dolby.interactiveplayer.R
import io.dolby.rtsviewer.uikit.text.Text

@Composable
fun LabelIndicator(modifier: Modifier, label: String?) {
    Box(
        modifier = modifier
            .padding(3.dp)
            .clip(shape = RoundedCornerShape(2.dp))
            .background(Color.Gray)
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(
            text = label?.uppercase() ?: stringResource(id = R.string.main_source_name),
            color = Color.White
        )
    }
}
