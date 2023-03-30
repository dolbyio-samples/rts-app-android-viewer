package io.dolby.rtsviewer.uikit.switch

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsToggleable
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performTouchInput
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
class SwitchComponentTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Before
    @Throws(Exception::class)
    fun setUp() {
        // Redirect Logcat to console
        ShadowLog.stream = System.out
    }

    @Test
    fun switchEnabledTest() {
        val firstChecked = mutableStateOf(false)
        var thirdChecked = mutableStateOf(false)
        composeTestRule.setContent {
            RTSViewerTheme {
                SwitchComponent(
                    text = "First",
                    isEnabled = false,
                    checked = firstChecked.value,
                    onCheckedChange = {
                        firstChecked.value = true
                    }
                )

                SwitchComponent(text = "Second", checked = false, onCheckedChange = {})

                SwitchComponent(
                    text = "Third",
                    isEnabled = true,
                    checked = thirdChecked.value,
                    onCheckedChange = {
                        thirdChecked.value = true
                    }
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("First Switch").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Second Switch").assertIsToggleable()
        assertFalse(firstChecked.value)

        composeTestRule.onNodeWithContentDescription("Second Switch").assertIsToggleable()

        composeTestRule.onNodeWithContentDescription("Third Switch").assertIsToggleable()
        composeTestRule.onNodeWithContentDescription("Third Switch").assertIsOff()
        composeTestRule.onNodeWithContentDescription("Third Switch").performTouchInput { click() }
        composeTestRule.onNodeWithContentDescription("Third Switch").assertIsOn()
        assertTrue(thirdChecked.value)
    }

    @Test
    fun switchChangeTest() {
        val firstChecked = mutableStateOf(false)
        composeTestRule.setContent {
            RTSViewerTheme {
                SwitchComponent(
                    text = "Test",
                    checked = firstChecked.value,
                    isEnabled = true,
                    onCheckedChange = {
                        firstChecked.value = it
                    }
                )
            }
        }
        assertFalse(firstChecked.value)
        composeTestRule.onNodeWithContentDescription("Test Switch").assertIsOff()
        composeTestRule.onNodeWithContentDescription("Test Switch").performTouchInput { click() }
        composeTestRule.onNodeWithContentDescription("Test Switch").assertIsOn()
        assertTrue(firstChecked.value)
    }
}
