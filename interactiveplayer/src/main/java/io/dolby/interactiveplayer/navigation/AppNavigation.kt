package io.dolby.interactiveplayer.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import io.dolby.interactiveplayer.detailInput.DetailInputScreen
import io.dolby.interactiveplayer.savedStreams.RecentStreamsScreen
import io.dolby.interactiveplayer.savedStreams.SavedStreamScreen
import io.dolby.interactiveplayer.settings.SettingsScreen
import io.dolby.interactiveplayer.streaming.multiview.MultiStreamingScreen
import io.dolby.interactiveplayer.streaming.multiview.SingleStreamingScreen

@Composable
fun AppNavigation(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.RecentStreams.route
    ) {
        composable(
            route = Screen.RecentStreams.route
        ) {
            RecentStreamsScreen(
                onPlayStream = { navController.navigate(Screen.MultiStreamingScreen.route(it)) },
                onPlayNewClick = { navController.navigate(Screen.DetailInputScreen.route) },
                onSavedStreamsClick = { navController.navigate(Screen.SavedStreams.route) },
                onSettingsClick = { navController.navigate(Screen.GlobalSettings.route) }
            )
        }

        composable(
            route = Screen.DetailInputScreen.route
        ) {
            DetailInputScreen(
                onPlayClick = { navController.navigate(Screen.MultiStreamingScreen.route(it)) },
                onSettingsClick = { navController.navigate(Screen.GlobalSettings.route) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.MultiStreamingScreen.route
        ) { entry ->
            val accountId = entry.arguments?.getString(Screen.MultiStreamingScreen.ARG_ACCOUNT_ID)
            val streamName = entry.arguments?.getString(Screen.MultiStreamingScreen.ARG_STREAM_NAME)
            if (streamName.isNullOrEmpty() || accountId.isNullOrEmpty()) {
                throw IllegalArgumentException()
            }
            MultiStreamingScreen(
                onBack = { navController.popBackStack() },
                onMainClick = { navController.navigate(Screen.SingleStreamingScreen.route) },
                onSettingsClick = { navController.navigate(Screen.StreamSettings.route(null)) }
            )
        }

        composable(
            route = Screen.SingleStreamingScreen.route
        ) {
            SingleStreamingScreen(
                onBack = {
                    navController.popBackStack()
                },
                onSettingsClick = { navController.navigate(Screen.StreamSettings.route(null)) }
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

        composable(
            route = Screen.GlobalSettings.route
        ) {
            SettingsScreen(
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = Screen.StreamSettings.route
        ) {
            val accountId = it.arguments?.getString(Screen.StreamSettings.ARG_ACCOUNT_ID)
            val streamName = it.arguments?.getString(Screen.StreamSettings.ARG_STREAM_NAME)
            if (streamName.isNullOrEmpty() || accountId.isNullOrEmpty()) {
                throw IllegalArgumentException()
            }
            SettingsScreen(
                onBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
