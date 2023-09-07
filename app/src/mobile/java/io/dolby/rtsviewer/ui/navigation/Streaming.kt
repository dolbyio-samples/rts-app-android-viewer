package io.dolby.rtsviewer.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import io.dolby.rtscomponentkit.domain.StreamingData
import io.dolby.rtsviewer.ui.streaming.multiview.ListViewScreen

@Composable
fun Streaming(navController: NavHostController) {
    ListViewScreen(
        onBack = { navController.popBackStack() },
        onMainClick = { navController.navigate(Screen.SingleStreamingScreen.route) })
}

fun streamingRoute() = Screen.MultiStreamingScreen.route

fun detailInputOnPlayClick(
    navController: NavHostController
): (StreamingData) -> Unit = {
    navController.navigate(Screen.MultiStreamingScreen.route(it))
}
