package io.dolby.rtsviewer.uikit.input

import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performTextInput
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
class TextInputTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Before
    @Throws(Exception::class)
    fun setUp() {
        // Redirect Logcat to console
        ShadowLog.stream = System.out
    }

    @Test
    fun textInputEnabledTest() {
        var firstValueChanged = false
        composeTestRule.setContent {
            RTSViewerTheme {
                TextInput(value = "First", enabled = false, onValueChange = {
                    firstValueChanged = true
                })

                TextInput(value = "Second", onValueChange = {})

                TextInput(value = "Third", enabled = true, onValueChange = {})
            }
        }

        composeTestRule.onNodeWithContentDescription("First Input").assertIsNotEnabled()
        assertFalse(firstValueChanged)

        composeTestRule.onNodeWithContentDescription("Second Input").assertIsEnabled()
        composeTestRule.onNodeWithContentDescription("Third Input").assertIsEnabled()
    }

    @Test
    fun textInputValueUpdateTest() {
        var valueChanged = false
        composeTestRule.setContent {
            RTSViewerTheme {
                TextInput(value = "Test", onValueChange = {
                    valueChanged = true
                }, label = "Test label")
            }
        }

        assertFalse(valueChanged)
        composeTestRule.onNodeWithContentDescription("Test label Input").performTextInput(" New update")
        composeTestRule.onNodeWithContentDescription("Test label Input").assert(hasText("Test New update"))
        assertTrue(valueChanged)
    }
}
