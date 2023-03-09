package io.dolby.rtsviewer.ui.detailInput

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import io.dolby.rtsviewer.uikit.theme.fontColor
import io.dolby.rtscomponentkit.domain.StreamingData
import io.dolby.rtsviewer.uikit.button.StyledButton
import io.dolby.rtsviewer.uikit.input.TextInput
import io.dolby.rtsviewer.uikit.utils.ViewState

@Composable
fun DetailInputScreen(onPlayClick: (StreamingData) -> Unit) {
    var streamName by remember { mutableStateOf("") }
    var accountId by remember { mutableStateOf("") }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.background)
    ) {
        ConstraintLayout(
            modifier = Modifier
                .size(size = 600.dp)
                .align(Alignment.Center)
                .padding(24.dp)
                .background(MaterialTheme.colors.background)
                .clip(MaterialTheme.shapes.large)
                .padding(25.dp)
        ) {
            val (
                header, title, subtitle, inputStream, inputAccount,
                savedStreamsButton, playButton, cleanStreamHistory
            ) = createRefs()
            Text(
                "Dolby.io Remote Monitor",
                style = MaterialTheme.typography.body1,
                color = fontColor(ViewState.Selected),
                textAlign = TextAlign.Center,
                modifier = Modifier.constrainAs(header) {
                    top.linkTo(parent.top, margin = 15.dp)
                    centerHorizontallyTo(parent)
                }
            )
            Text(
                "View Stream",
                style = MaterialTheme.typography.h1,
                color = fontColor(ViewState.Selected),
                textAlign = TextAlign.Center,
                modifier = Modifier.constrainAs(title) {
                    top.linkTo(header.bottom, margin = 20.dp)
                    centerHorizontallyTo(parent)
                }
            )
            Text(
                "Enter your stream name and publisher \naccount ID to view a stream",
                style = MaterialTheme.typography.body1,
                color = fontColor(ViewState.Selected),
                textAlign = TextAlign.Center,
                modifier = Modifier.constrainAs(subtitle) {
                    top.linkTo(title.bottom, margin = 12.dp)
                    centerHorizontallyTo(parent)
                }
            )
            TextInput(
                value = streamName,
                label = "Enter your stream name",
                onValueChange = {
                    streamName = it
                },
                modifier = Modifier.constrainAs(inputStream) {
                    top.linkTo(subtitle.bottom, margin = 25.dp)
                }
            )
            TextInput(
                value = accountId,
                label = "Enter your account ID",
                onValueChange = {
                    accountId = it
                },
                modifier = Modifier.constrainAs(inputAccount) {
                    top.linkTo(inputStream.bottom, margin = 12.dp)
                }
            )
            StyledButton(
                buttonText = "Play",
                modifier = Modifier.constrainAs(playButton) {
                    top.linkTo(inputAccount.bottom, margin = 12.dp)
                    centerHorizontallyTo(parent)
                },
                onClickAction = {
                    onPlayClick(
                        StreamingData(
                            streamName = streamName,
                            accountId = accountId
                        )
                    )
                },
                isPrimary = true,
                isEnabled = streamName.trim().isNotEmpty() && accountId.trim().isNotEmpty()
            )
        }
    }
}
