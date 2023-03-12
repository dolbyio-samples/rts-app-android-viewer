package io.dolby.rtsviewer.uikit.button

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.dolby.rtsviewer.uikit.theme.RTSViewerTheme
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
        composeTestRule.setContent {
            RTSViewerTheme {
                StyledButton(buttonText = "First button", isEnabled = true, onClickAction = {

                })

                StyledButton(buttonText = "Second button", onClickAction = {

                })
            }
        }

        composeTestRule.onNodeWithText("First button").performClick()

        composeTestRule.onNodeWithText("Second button").assertExists()
    }
}