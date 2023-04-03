package io.dolby.rtsviewer.uikit.button

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import io.dolby.rtsviewer.uikit.theme.RTSViewerTheme
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowLog

@RunWith(RobolectricTestRunner::class)
class StateButtonTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Before
    @Throws(Exception::class)
    fun setUp() {
        // Redirect Logcat to console
        ShadowLog.stream = System.out
    }

    @Test
    fun stateButtonEnabledTest() {
        var firstClicked = false
        var thirdClicked = false
        composeTestRule.setContent {
            RTSViewerTheme {
                StateButton(text = "First", stateText = "State 1", isEnabled = false, onClick = {
                    firstClicked = true
                })

                StateButton(text = "Second", stateText = "State 2")

                StateButton(text = "Third", stateText = "State 3", isEnabled = true, onClick = {
                    thirdClicked = true
                })
            }
        }

        composeTestRule.onNodeWithContentDescription("First State Button").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("First State Button").assertHasNoClickAction()
        assertFalse(firstClicked)

        composeTestRule.onNodeWithContentDescription("Second State Button").assertHasClickAction()

        composeTestRule.onNodeWithContentDescription("Third State Button").assertHasClickAction()
        composeTestRule.onNodeWithContentDescription("Third State Button").performClick()
        assertTrue(thirdClicked)
    }

    @Test
    fun stateButtonClickTest() {
        var firstClicked = false
        composeTestRule.setContent {
            RTSViewerTheme {
                StateButton(text = "Test", stateText = "", onClick = {
                    firstClicked = true
                })
            }
        }
        assertFalse(firstClicked)
        composeTestRule.onNodeWithContentDescription("Test State Button").performClick()
        assertTrue(firstClicked)
    }
}
