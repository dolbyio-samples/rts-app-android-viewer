package io.dolby.rtsviewer.ui.navigation

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavBackStackEntry
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.dolby.rtsviewer.data.StreamingData
import io.dolby.rtsviewer.ui.detailInput.DetailInputScreen
import io.dolby.rtsviewer.ui.streaming.StreamingScreen

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = Screen.DetailInputScreen.route,
    ) {
        composable(
            route = Screen.DetailInputScreen.route,
        ) {
            DetailInputScreen {
                navController.navigate(Screen.StreamingScreen.route(it))
            }
        }

        composable(
            route = Screen.StreamingScreen.route
        ) {
            val streamName = it.arguments?.getString(Screen.StreamingScreen.ARG_STREAM_NAME)
            val accountId = it.arguments?.getString(Screen.StreamingScreen.ARG_ACCOUNT_ID)
            StreamingScreen(
                StreamingData(
                    streamName = streamName ?: "", accountId = accountId ?: ""
                )
            )
        }
    }
}
