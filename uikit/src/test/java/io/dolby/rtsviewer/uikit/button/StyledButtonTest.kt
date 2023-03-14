package io.dolby.rtsviewer.uikit.button

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
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
class StyledButtonTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Before
    @Throws(Exception::class)
    fun setUp() {
        // Redirect Logcat to console
        ShadowLog.stream = System.out
    }

    @Test
    fun buttonEnabledTest() {
        var firstClicked = false
        composeTestRule.setContent {
            RTSViewerTheme {
                StyledButton(buttonText = "First", isEnabled = false, onClickAction = {
                    firstClicked = true
                })

                StyledButton(buttonText = "Second")

                StyledButton(buttonText = "Third", isEnabled = true)
            }
        }

        composeTestRule.onNodeWithContentDescription("First Button").assertIsNotEnabled()
        assertFalse(firstClicked)

        composeTestRule.onNodeWithContentDescription("Second Button").assertIsEnabled()
        composeTestRule.onNodeWithContentDescription("Third Button").assertIsEnabled()
    }

    @Test
    fun buttonClickTest() {
        var firstClicked = false
        composeTestRule.setContent {
            RTSViewerTheme {
                StyledButton(buttonText = "Test", onClickAction = {
                    firstClicked = true
                })
            }
        }

        assertFalse(firstClicked)
        composeTestRule.onNodeWithContentDescription("Test Button").performClick()
        assertTrue(firstClicked)
    }
}
