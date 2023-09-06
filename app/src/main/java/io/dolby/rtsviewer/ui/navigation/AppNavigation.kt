package io.dolby.rtsviewer.ui.navigation

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import io.dolby.rtsviewer.ui.detailInput.DetailInputScreen
import io.dolby.rtsviewer.ui.savedStreams.SavedStreamScreen
import io.dolby.rtsviewer.ui.streaming.multiview.ListViewScreen
import io.dolby.rtsviewer.ui.streaming.SingleStreamingScreen
import io.dolby.rtsviewer.utils.isTV

@OptIn(ExperimentalAnimationApi::class)
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
                onPlayClick = {
                    if (isTV(navController.context)) {
                        navController.navigate(Screen.StreamingScreen.route(it))
                    } else {
                        navController.navigate(Screen.MultiStreamingScreen.route(it))
                    }
                },
                onSavedStreamsClick = {
                    navController.navigate(Screen.SavedStreams.route)
                }
            )
        }

        composable(
            route = Screen.MultiStreamingScreen.route
        ) {
            val accountId = it.arguments?.getString(Screen.StreamingScreen.ARG_ACCOUNT_ID)
            val streamName = it.arguments?.getString(Screen.StreamingScreen.ARG_STREAM_NAME)
            if (streamName.isNullOrEmpty() || accountId.isNullOrEmpty()) {
                throw IllegalArgumentException()
            }
//            StreamingScreen(onBack = { navController.popBackStack() })
            ListViewScreen(
                onBack = { navController.popBackStack() },
                onMainClick = { navController.navigate(Screen.SingleStreamingScreen.route) })
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
