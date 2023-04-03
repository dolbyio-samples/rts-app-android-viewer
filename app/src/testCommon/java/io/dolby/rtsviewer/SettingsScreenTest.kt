package io.dolby.rtsviewer

import android.view.KeyEvent
import android.view.KeyEvent.ACTION_DOWN
import android.view.KeyEvent.ACTION_UP
import android.view.KeyEvent.KEYCODE_BACK
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performKeyPress
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import org.junit.Rule
import org.junit.Test

abstract class SettingsScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun givenSettingsScreen_whenClickBack_thenStreamingScreenShown() {
        // Given
        openStreamingScreen(composeTestRule)
        showToolbar(composeTestRule)
        performClickOnSettingsButton(composeTestRule)
        // When
        composeTestRule.onNodeWithContentDescription("Settings Screen").assertIsDisplayed()
        performCloseSettings(composeTestRule)

        // Then
        composeTestRule.onNodeWithContentDescription("Streaming Screen").assertIsDisplayed()
    }

    @Test
    fun givenSettingsScreen_thenLiveIndicatorSwitchShown() {
        // Given
        openStreamingScreen(composeTestRule)
        showToolbar(composeTestRule)
        performClickOnSettingsButton(composeTestRule)

        // Then
        composeTestRule.onNodeWithContentDescription("Live indicator Switch").assertIsDisplayed()
    }

    @Test
    fun givenSettingsScreenForOfflineStream_whenLiveIndicatorToggledOff_thenLiveIndicatorNotShown() {
        // Given
        openStreamingScreen(composeTestRule)
        showToolbar(composeTestRule)
        performClickOnSettingsButton(composeTestRule)
        // When
        composeTestRule.onNodeWithContentDescription("Live indicator Switch").performScrollTo()
            .performTouchInput { click() }
        composeTestRule.onNodeWithContentDescription("Live indicator Switch").assertIsOff()
        performCloseSettings(composeTestRule)

        // Then
        composeTestRule.onNodeWithContentDescription("Offline Status Indicator").assertDoesNotExist()
    }
}

private fun performCloseSettings(composeTestRule: ComposeContentTestRule) {
    composeTestRule.apply {
        onNodeWithContentDescription("Settings Screen").performKeyPress(
            keyEvent = androidx.compose.ui.input.key.KeyEvent(
                KeyEvent(ACTION_DOWN, KEYCODE_BACK)
            )
        )
        onNodeWithContentDescription("Settings Screen").performKeyPress(
            keyEvent = androidx.compose.ui.input.key.KeyEvent(
                KeyEvent(ACTION_UP, KEYCODE_BACK)
            )
        )
    }
}
