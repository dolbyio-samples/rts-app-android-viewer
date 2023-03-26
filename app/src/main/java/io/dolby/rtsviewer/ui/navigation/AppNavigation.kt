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
            val streamNameToPlay = backStackEntry.savedStateHandle?.get<String>(Screen.DetailInputScreen.ARG_STREAM_NAME_TO_PLAY)
            val accountIDToPlay = backStackEntry.savedStateHandle?.get<String>(Screen.DetailInputScreen.ARG_ACCOUNT_ID_TO_PLAY)
            var streamingData: StreamingData? = null

            if (streamNameToPlay != null && accountIDToPlay != null) {
                streamingData = StreamingData(accountId = accountIDToPlay, streamName = streamNameToPlay)

                // Clear saved state data
                backStackEntry.savedStateHandle?.remove<String>(Screen.DetailInputScreen.ARG_STREAM_NAME_TO_PLAY)
                backStackEntry.savedStateHandle?.remove<String>(Screen.DetailInputScreen.ARG_ACCOUNT_ID_TO_PLAY)
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
            if (streamName.isNullOrEmpty() || accountId.isNullOrEmpty()) {
                throw IllegalArgumentException()
            }
            StreamingScreen()
        }

        composable(
            route = Screen.SavedStreams.route
        ) {
            SavedStreamScreen(
                onPlayStream = { streamDetail ->
                    navController.previousBackStackEntry
                        ?.savedStateHandle?.let {
                            it[Screen.DetailInputScreen.ARG_STREAM_NAME_TO_PLAY] = streamDetail.streamName
                            it[Screen.DetailInputScreen.ARG_ACCOUNT_ID_TO_PLAY] = streamDetail.accountID
                        }

                    navController.popBackStack()
                }
            )
        }
    }
}
