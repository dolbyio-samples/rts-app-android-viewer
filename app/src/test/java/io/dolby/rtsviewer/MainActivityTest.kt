package io.dolby.rtsviewer

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ActivityScenario
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLog

@Config(instrumentedPackages = ["androidx.loader.content"])
@RunWith(RobolectricTestRunner::class)
class MainActivityTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Before
    @Throws(Exception::class)
    fun setUp() {
        // Redirect Logcat to console
        ShadowLog.stream = System.out
    }

    @Test
    fun `given DetailInputScreen with unfilled inputs then Play button is disabled`() {
        ActivityScenario.launch(MainActivity::class.java)
            .use { scenario ->
                scenario.onActivity { activity: MainActivity ->
                    composeTestRule
                        .onNodeWithContentDescription("Play Button").assertIsNotEnabled()
                }
            }
    }

    @Test
    fun `when Stream name is not empty then Play button is enabled`() {
        ActivityScenario.launch(MainActivity::class.java)
            .use { scenario ->
                scenario.onActivity { activity: MainActivity ->
                    composeTestRule.onNodeWithContentDescription("Enter your stream name Input")
                        .performTextInput("StreamName")
                    composeTestRule.onNodeWithContentDescription("Enter your account ID Input")
                        .performTextInput("AccountId")
                    composeTestRule.onNodeWithContentDescription("Play Button").assertIsEnabled()
                }
            }
    }
}
