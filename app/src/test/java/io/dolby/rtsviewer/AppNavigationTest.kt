package io.dolby.rtsviewer

import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.testing.TestNavHostController
import io.dolby.rtsviewer.ui.navigation.AppNavigation
import io.dolby.rtsviewer.ui.navigation.Screen
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLog

@Config(instrumentedPackages = ["androidx.loader.content"])
@RunWith(RobolectricTestRunner::class)
class AppNavigationTest {
    @get:Rule
    val composeTestRule = createComposeRule()
    lateinit var navController: TestNavHostController

    @Before
    @Throws(Exception::class)
    fun setUp() {
        // Redirect Logcat to console
        ShadowLog.stream = System.out
    }

    @Before
    fun setupAppNavHost() {
        composeTestRule.setContent {
            navController = TestNavHostController(LocalContext.current)
            navController.navigatorProvider.addNavigator(ComposeNavigator())
            AppNavigation(navController = navController)
        }
    }

    @Test
    fun givenAppNavHost_thenValidStartDestination() {
        composeTestRule
            .onNodeWithContentDescription("Detail Input Screen")
            .assertIsDisplayed()

        assertEquals(
            Screen.DetailInputScreen.route,
            navController.currentBackStackEntry?.destination?.route
        )
    }

    @Test
    fun givenAppNavHost_whenClickPlay_thenNavigateToStreaming() {
        composeTestRule.onNodeWithContentDescription("Enter your stream name Input")
            .performTextInput("StreamName")
        composeTestRule.onNodeWithContentDescription("Enter your account ID Input")
            .performTextInput("AccountID")

        composeTestRule.onNodeWithContentDescription("Play Button").performScrollTo()
        composeTestRule.onNodeWithContentDescription("Play Button").assertIsEnabled()
        composeTestRule.onNodeWithContentDescription("Play Button").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Play Button").performClick()

        val route = navController.currentDestination?.route
        assertEquals(Screen.StreamingScreen.route, route)
    }
}