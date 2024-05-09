package io.dolby.interactiveplayer.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.dolby.interactiveplayer.datastore.StreamDetail
import io.dolby.rtscomponentkit.domain.StreamingData

fun streamingDataFrom(streamDetail: StreamDetail): StreamingData {
    return StreamingData(
        accountId = streamDetail.accountID,
        streamName = streamDetail.streamName
    )
}

@Composable
fun horizontalPaddingDp(): Dp {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp
    return if (screenWidth <= 600) 15.dp else ((screenWidth - 450) / 2).dp
}
