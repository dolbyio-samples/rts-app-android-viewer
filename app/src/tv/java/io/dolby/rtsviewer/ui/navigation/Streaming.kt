package io.dolby.rtsviewer.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import io.dolby.rtscomponentkit.domain.StreamingData
import io.dolby.rtsviewer.ui.streaming.StreamingScreen

@Composable
fun Streaming(navController: NavHostController) {
    StreamingScreen(onBack = { navController.popBackStack() })
}

fun streamingRoute() = Screen.StreamingScreen.route

fun detailInputOnPlayClick(
    navController: NavHostController
): (StreamingData) -> Unit = {
    navController.navigate(Screen.StreamingScreen.route(it))
}
