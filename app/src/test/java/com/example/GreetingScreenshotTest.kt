package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.assistant.AssistantState
import com.example.ui.Character3D
import com.example.ui.GlowingOrb
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class GreetingScreenshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun rashed_glowing_orb_screenshot() {
        composeTestRule.setContent {
            MyApplicationTheme {
                GlowingOrb(
                    state = AssistantState.Listening,
                    amplitude = 0.5f
                )
            }
        }

        composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/greeting.png")
    }

    @Test
    fun rashed_3d_character_screenshot() {
        composeTestRule.setContent {
            MyApplicationTheme {
                Character3D(
                    state = AssistantState.Speaking,
                    amplitude = 0.65f
                )
            }
        }

        composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/character_3d.png")
    }
}
