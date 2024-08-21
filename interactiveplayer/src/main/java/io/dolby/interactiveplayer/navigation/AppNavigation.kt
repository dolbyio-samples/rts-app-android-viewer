package io.dolby.interactiveplayer.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import io.dolby.interactiveplayer.detailInput.DetailInputScreen
import io.dolby.interactiveplayer.savedStreams.RecentStreamsScreen
import io.dolby.interactiveplayer.savedStreams.SavedStreamScreen
import io.dolby.interactiveplayer.settings.SettingsScreen
import io.dolby.interactiveplayer.streaming.multiview.MultiStreamingScreen
import io.dolby.interactiveplayer.streaming.multiview.SingleStreamingScreen
import io.dolby.interactiveplayer.utils.EnablePipMode
import io.dolby.rtscomponentkit.domain.StreamingData

@Composable
fun AppNavigation(
    navController: NavHostController,
    appViewModel: AppViewModel = hiltViewModel()
) {
    val currentShouldEnterPipMode by appViewModel.isPipEnabled.collectAsStateWithLifecycle()
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
                appViewModel = appViewModel,
                onBack = {
                    navController.popBackStack()
                    appViewModel.enablePip(false)
                },
                onMainClick = {
                    navController.navigate(
                        Screen.SingleStreamingScreen.route(
                            StreamingData(accountId, streamName)
                        )
                    )
                },
                onSettingsClick = { navController.navigate(Screen.StreamSettings.route(StreamingData(accountId, streamName))) }
            )
        }

        composable(
            route = Screen.SingleStreamingScreen.route
        ) { entry ->
            val accountId = entry.arguments?.getString(Screen.MultiStreamingScreen.ARG_ACCOUNT_ID)
            val streamName = entry.arguments?.getString(Screen.MultiStreamingScreen.ARG_STREAM_NAME)
            if (streamName.isNullOrEmpty() || accountId.isNullOrEmpty()) {
                throw IllegalArgumentException()
            }
            SingleStreamingScreen(
                appViewModel = appViewModel,
                onBack = {
                    navController.popBackStack()
                    appViewModel.enablePip(false)
                },
                onSettingsClick = { navController.navigate(Screen.StreamSettings.route(StreamingData(accountId, streamName))) }
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
    EnablePipMode(enablePipMode = currentShouldEnterPipMode)
}
