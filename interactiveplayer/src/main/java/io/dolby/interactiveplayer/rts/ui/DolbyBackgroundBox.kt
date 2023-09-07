package io.dolby.interactiveplayer.rts.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import io.dolby.interactiveplayer.R

@Composable
fun DolbyBackgroundBox(
    showGradientBackground: Boolean = true,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.background)
    ) {
        if (showGradientBackground) {
            Image(
                painter = painterResource(id = R.drawable.gradient_background),
                contentDescription = stringResource(id = R.string.gradient_background_image),
                modifier = Modifier
                    .matchParentSize(),
                contentScale = ContentScale.Crop
            )
        }

        content()
    }
}
