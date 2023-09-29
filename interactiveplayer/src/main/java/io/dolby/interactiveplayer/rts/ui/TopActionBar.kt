package io.dolby.interactiveplayer.rts.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import io.dolby.interactiveplayer.R

@Composable
fun TopActionBar(onActionClick: (() -> Unit)? = null, modifier: Modifier = Modifier) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .padding(3.dp)
            .background(MaterialTheme.colors.background)
    ) {
        Image(
            painter = painterResource(id = R.drawable.dolby_logo),
            contentDescription = "Dolby Logo",
            colorFilter = ColorFilter.tint(MaterialTheme.colors.onBackground),
            modifier = Modifier
                .align(Alignment.Center)
                .size(width = 26.dp, height = 18.dp)
        )
        onActionClick?.let {
            Image(
                painter = painterResource(id = io.dolby.uikit.R.drawable.ic_settings),
                contentDescription = "Settings",
                colorFilter = ColorFilter.tint(MaterialTheme.colors.onBackground),
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .size(width = 26.dp, height = 26.dp)
                    .clickable { onActionClick() }
            )
        }
    }
}
