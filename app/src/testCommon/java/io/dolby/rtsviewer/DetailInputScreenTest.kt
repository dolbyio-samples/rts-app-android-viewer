package io.dolby.rtsviewer

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ActivityScenario
import org.junit.Rule
import org.junit.Test

abstract class DetailInputScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun givenMainActivity_thenDetailInputScreenShown() {
        // Given
        ActivityScenario.launch(MainActivity::class.java)
        // Then
        composeTestRule
            .onNodeWithContentDescription("Stream Detail Input Screen").assertIsDisplayed()
    }

    @Test
    fun givenDetailInputScreenWithUnfilledInputs_whenTappingThePlayButton_thenShowsValidationDialog() {
        // Given
        ActivityScenario.launch(MainActivity::class.java)
        // When
        composeTestRule
            .onNodeWithContentDescription("Play Button").performScrollTo().performClick()

        // Then
        composeTestRule.onNodeWithContentDescription("Validation Alert").assertIsDisplayed()
    }

    @Test
    fun givenDetailInputScreen_whenFillInputsAndTapThePlayButton_thenStreamingScreenOpen() {
        // Given
        ActivityScenario.launch(MainActivity::class.java)
        // When
        composeTestRule
            .onNodeWithContentDescription("Enter your stream name Input").performTextInput("StreamName")
        composeTestRule
            .onNodeWithContentDescription("Enter your account ID Input").performTextInput("AccountId")
        composeTestRule
            .onNodeWithContentDescription("Play Button").performScrollTo().performClick()
        // Then
        composeTestRule.onNodeWithContentDescription("Streaming Screen").assertIsDisplayed()
    }
}
