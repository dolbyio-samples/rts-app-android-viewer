package io.dolby.interactiveplayer.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import io.dolby.interactiveplayer.detailInput.DetailInputScreen
import io.dolby.interactiveplayer.savedStreams.SavedStreamScreen
import io.dolby.interactiveplayer.streaming.multiview.ListViewScreen
import io.dolby.interactiveplayer.streaming.multiview.SingleStreamingScreen

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
                onPlayClick = { navController.navigate(Screen.MultiStreamingScreen.route(it)) },
                onSavedStreamsClick = {
                    navController.navigate(Screen.SavedStreams.route)
                }
            )
        }

        composable(
            route = Screen.MultiStreamingScreen.route
        ) {
            val accountId = it.arguments?.getString(Screen.MultiStreamingScreen.ARG_ACCOUNT_ID)
            val streamName = it.arguments?.getString(Screen.MultiStreamingScreen.ARG_STREAM_NAME)
            if (streamName.isNullOrEmpty() || accountId.isNullOrEmpty()) {
                throw IllegalArgumentException()
            }
            ListViewScreen(
                onBack = { navController.popBackStack() },
                onMainClick = { navController.navigate(Screen.SingleStreamingScreen.route) }
            )
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
