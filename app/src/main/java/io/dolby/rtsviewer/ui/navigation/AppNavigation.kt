package io.dolby.rtsviewer.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import io.dolby.rtscomponentkit.domain.StreamingData
import io.dolby.rtsviewer.ui.detailInput.DetailInputScreen
import io.dolby.rtsviewer.ui.savedStreams.SavedStreamScreen
import io.dolby.rtsviewer.ui.streaming.StreamingScreen

@Composable
fun AppNavigation(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.DetailInputScreen.route
    ) {
        composable(
            route = Screen.DetailInputScreen.route
        ) { backStackEntry ->
            val streamNameToPlay =
                backStackEntry.savedStateHandle.get<String>(Screen.DetailInputScreen.ARG_STREAM_NAME_TO_PLAY)
            val accountIDToPlay =
                backStackEntry.savedStateHandle.get<String>(Screen.DetailInputScreen.ARG_ACCOUNT_ID_TO_PLAY)
            val useDevEnv =
                backStackEntry.savedStateHandle.get<Boolean>(Screen.DetailInputScreen.ARG_USE_DEV_ENV)
            val disableAudio =
                backStackEntry.savedStateHandle.get<Boolean>(Screen.DetailInputScreen.ARG_DISABLE_AUDIO)
            val rtcLogs =
                backStackEntry.savedStateHandle.get<Boolean>(Screen.DetailInputScreen.ARG_RTC_LOGS)
            val videoJitterMinimumDelayMs =
                backStackEntry.savedStateHandle.get<Int>(Screen.DetailInputScreen.ARG_RTC_LOGS)
            var streamingData: StreamingData? = null

            if (streamNameToPlay != null && accountIDToPlay != null) {
                streamingData = StreamingData(
                    accountId = accountIDToPlay,
                    streamName = streamNameToPlay,
                    useDevEnv = useDevEnv ?: false,
                    disableAudio = disableAudio ?: false,
                    rtcLogs = rtcLogs ?: false,
                    videoJitterMinimumDelayMs = videoJitterMinimumDelayMs ?: 0
                )

                // Clear saved state data
                backStackEntry.savedStateHandle.remove<String>(Screen.DetailInputScreen.ARG_STREAM_NAME_TO_PLAY)
                backStackEntry.savedStateHandle.remove<String>(Screen.DetailInputScreen.ARG_ACCOUNT_ID_TO_PLAY)
                backStackEntry.savedStateHandle.remove<Boolean>(Screen.DetailInputScreen.ARG_USE_DEV_ENV)
                backStackEntry.savedStateHandle.remove<Boolean>(Screen.DetailInputScreen.ARG_DISABLE_AUDIO)
                backStackEntry.savedStateHandle.remove<Boolean>(Screen.DetailInputScreen.ARG_RTC_LOGS)
            }

            DetailInputScreen(
                streamingData = streamingData,
                onPlayClick = {
                    navController.navigate(Screen.StreamingScreen.route(it))
                },
                onSavedStreamsClick = {
                    navController.navigate(Screen.SavedStreams.route)
                }
            )
        }

        composable(
            route = Screen.StreamingScreen.route
        ) {
            val accountId = it.arguments?.getString(Screen.StreamingScreen.ARG_ACCOUNT_ID)
            val streamName = it.arguments?.getString(Screen.StreamingScreen.ARG_STREAM_NAME)
            val useDevEnv = it.arguments?.getBoolean(Screen.DetailInputScreen.ARG_USE_DEV_ENV)
            val disableAudio = it.arguments?.getBoolean(Screen.DetailInputScreen.ARG_DISABLE_AUDIO)
            val rtcLogs = it.arguments?.getBoolean(Screen.DetailInputScreen.ARG_RTC_LOGS)
            if (streamName.isNullOrEmpty() || accountId.isNullOrEmpty()) {
                throw IllegalArgumentException()
            }
            StreamingScreen(onBack = { navController.popBackStack() })
        }

        composable(
            route = Screen.SavedStreams.route
        ) {
            SavedStreamScreen(
                onPlayStream = { streamDetail ->
                    navController.previousBackStackEntry
                        ?.savedStateHandle?.let {
                            it[Screen.DetailInputScreen.ARG_STREAM_NAME_TO_PLAY] =
                                streamDetail.streamName
                            it[Screen.DetailInputScreen.ARG_ACCOUNT_ID_TO_PLAY] =
                                streamDetail.accountID
                        }

                    navController.popBackStack()
                },
                onBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
