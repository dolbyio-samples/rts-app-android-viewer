package io.dolby.rtsviewer.uikit.button

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
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
class StyledIconButtonTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Before
    @Throws(Exception::class)
    fun setUp() {
        // Redirect Logcat to console
        ShadowLog.stream = System.out
    }

    @Test
    fun iconButtonEnabledTest() {
        var firstClicked = false
        val painter = ColorPainter(color = Color.Red)
        composeTestRule.setContent {
            RTSViewerTheme {
                StyledIconButton(icon = painter, text = "First", enabled = false, onClick = {
                    firstClicked = true
                })

                StyledIconButton(icon = painter, text = "Second")

                StyledIconButton(icon = painter, text = "Third", enabled = true)
            }
        }

        composeTestRule.onNodeWithContentDescription("First Icon Button").assertIsNotEnabled()
        assertFalse(firstClicked)

        composeTestRule.onNodeWithContentDescription("Second Icon Button").assertIsEnabled()
        composeTestRule.onNodeWithContentDescription("Third Icon Button").assertIsEnabled()
    }

    @Test
    fun iconButtonClickTest() {
        var firstClicked = false
        val painter = ColorPainter(color = Color.Red)
        composeTestRule.setContent {
            RTSViewerTheme {
                StyledIconButton(icon = painter, text = "Test", onClick = {
                    firstClicked = true
                })
            }
        }
        assertFalse(firstClicked)
        composeTestRule.onNodeWithContentDescription("Test Icon Button").performClick()
        assertTrue(firstClicked)
    }
}
