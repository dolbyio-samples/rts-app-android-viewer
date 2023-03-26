package io.dolby.rtsviewer.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavDirections
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import io.dolby.rtsviewer.datastore.StreamDetail
import io.dolby.rtsviewer.ui.detailInput.DetailInputScreen
import io.dolby.rtsviewer.ui.savedStreams.SavedStreamScreen
import io.dolby.rtsviewer.ui.streaming.StreamingScreen

private const val STREAM_DETAIL_TO_PLAY = "streamDetailToPlay"
@Composable
fun AppNavigation(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.DetailInputScreen.route
    ) {
        composable(
            route = Screen.DetailInputScreen.route
        ) {
            var streamDetail: StreamDetail? = null
            if (navController.currentBackStackEntry?.savedStateHandle?.contains(STREAM_DETAIL_TO_PLAY) == true) {
                streamDetail = navController.currentBackStackEntry!!.savedStateHandle.get<StreamDetail>(STREAM_DETAIL_TO_PLAY)
            }

            DetailInputScreen(
                selectedStreamDetail = streamDetail,
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
                        ?.savedStateHandle
                        ?.set(STREAM_DETAIL_TO_PLAY, streamDetail)

                    navController.popBackStack()
                }
            )
        }
    }
}
