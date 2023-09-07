package io.dolby.rtsviewer.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import io.dolby.rtsviewer.ui.detailInput.DetailInputScreen
import io.dolby.rtsviewer.ui.savedStreams.SavedStreamScreen
import io.dolby.rtsviewer.ui.streaming.SingleStreamingScreen

@Composable
fun AppNavigation(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.DetailInputScreen.route
    ) {
        composable(
            route = Screen.DetailInputScreen.route
        ) {
            DetailInputScreen(
                onPlayClick = detailInputOnPlayClick(navController),
                onSavedStreamsClick = {
                    navController.navigate(Screen.SavedStreams.route)
                }
            )
        }

        composable(
            route = streamingRoute()
        ) {
            val accountId = it.arguments?.getString(Screen.StreamingScreen.ARG_ACCOUNT_ID)
            val streamName = it.arguments?.getString(Screen.StreamingScreen.ARG_STREAM_NAME)
            if (streamName.isNullOrEmpty() || accountId.isNullOrEmpty()) {
                throw IllegalArgumentException()
            }
            Streaming(navController)
        }

        composable(
            route = Screen.SingleStreamingScreen.route
        ) {
            SingleStreamingScreen(
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = Screen.SavedStreams.route
        ) {
            SavedStreamScreen(
                onPlayStream = { streamDetail ->
                    navController.navigate(Screen.MultiStreamingScreen.route(streamDetail))
                },
                onBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}

