package io.dolby.rtsviewer.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import io.dolby.rtsviewer.ui.detailInput.DetailInputScreen
import io.dolby.rtsviewer.ui.streaming.StreamingScreen

@Composable
fun AppNavigation(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.DetailInputScreen.route
    ) {
        composable(
            route = Screen.DetailInputScreen.route
        ) {
            DetailInputScreen {
                navController.navigate(Screen.StreamingScreen.route(it))
            }
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
    }
}
