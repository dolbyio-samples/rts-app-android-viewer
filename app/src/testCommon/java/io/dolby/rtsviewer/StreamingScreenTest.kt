package io.dolby.rtsviewer

import android.view.KeyEvent
import android.view.KeyEvent.ACTION_DOWN
import android.view.KeyEvent.ACTION_UP
import android.view.KeyEvent.KEYCODE_DPAD_CENTER
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyPress
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ActivityScenario
import org.junit.Rule
import org.junit.Test

abstract class StreamingScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun givenStreamingScreen_thenStatusIndicatorIsDisplayed() {
        // Given
        openStreamingScreen(composeTestRule)
        // Then
        composeTestRule.onNodeWithContentDescription("Offline Status Indicator").assertIsDisplayed()
    }

    @Test
    fun givenStreamingScreen_whenPerformDPadCenterClick_thenSettingsButtonIsShown() {
        // Given
        openStreamingScreen(composeTestRule)
        // When
        showToolbar(composeTestRule)
        // Then
        composeTestRule.onNodeWithContentDescription("Settings Icon Button").assertIsDisplayed()
    }

    @Test
    fun givenStreamingScreenWithToolbar_whenClickOnSettings_thenSettingsScreenIsShown() {
        // Given
        openStreamingScreen(composeTestRule)
        showToolbar(composeTestRule)
        // When
        performClickOnSettingsButton(composeTestRule)
        // Then
        composeTestRule.onNodeWithContentDescription("Settings Screen").assertIsDisplayed()
    }
}
fun openStreamingScreen(composeTestRule: ComposeContentTestRule) {
    ActivityScenario.launch(MainActivity::class.java)
    composeTestRule.apply {
        onNodeWithContentDescription("Enter your stream name Input").performTextInput("StreamName")
        onNodeWithContentDescription("Enter your account ID Input").performTextInput("AccountId")
        onNodeWithContentDescription("Play Button").performScrollTo().performClick()

        onNodeWithContentDescription("Streaming Screen").assertIsDisplayed()
    }
}
fun showToolbar(composeTestRule: ComposeContentTestRule) {
    composeTestRule.apply {
        onNodeWithContentDescription("Streaming Toolbar").performKeyPress(
            keyEvent = androidx.compose.ui.input.key.KeyEvent(
                KeyEvent(ACTION_DOWN, KEYCODE_DPAD_CENTER)
            )
        )
        onNodeWithContentDescription("Streaming Toolbar").performKeyPress(
            keyEvent = androidx.compose.ui.input.key.KeyEvent(
                KeyEvent(ACTION_UP, KEYCODE_DPAD_CENTER)
            )
        )
    }
}

fun performClickOnSettingsButton(composeTestRule: ComposeContentTestRule) {
    composeTestRule.apply {
        onNodeWithContentDescription("Settings Icon Button").performClick()
    }
}
