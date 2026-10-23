package com.jaarvis

import android.graphics.Bitmap
import java.io.File
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CommandCenterTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    private fun capture(name: String) {
        val directory = File(rule.activity.getExternalFilesDir(null), "screenshots").apply { mkdirs() }
        File(directory, "$name.png").outputStream().use { stream ->
            check(rule.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, stream))
        }
    }

    @Test fun homeRendersAndCanBeCaptured() {
        rule.onNodeWithContentDescription("Activate voice command").assertExists()
        rule.onNodeWithText("JAARVIS").assertExists()
        capture("home")
    }

    @Test fun everyModuleOpensAndBackReturnsHome() {
        listOf("intelligence", "operations", "research", "memory", "engineering", "finance", "system", "automations").forEach { name ->
            rule.onNodeWithContentDescription("Open $name module").performScrollTo().performClick()
            rule.onNodeWithText(name.uppercase()).assertExists()
            capture(name)
            rule.onNodeWithText("‹  BACK").performClick()
            rule.onNodeWithText("ONE CORE. EIGHT CONNECTIONS.").assertExists()
        }
    }

    @Test fun typedBatteryCommandReturnsPlatformReading() {
        rule.onNodeWithText("Enter a local command").performTextInput("battery")
        rule.onNodeWithContentDescription("Send command").performClick()
        rule.onNodeWithText("Battery level is", substring = true).assertExists()
    }

    @Test fun notesAndPreferencesHaveWorkingControls() {
        rule.onNodeWithContentDescription("Open memory module").performScrollTo().performClick()
        rule.onNodeWithText("Local note · saved automatically").performTextClearance()
        rule.onNodeWithText("Local note · saved automatically").performTextInput("Test memory")
        rule.onNodeWithText("Test memory").assertExists()
        rule.onNodeWithText("Clear note").performScrollTo().performClick()
        rule.onNodeWithText("‹  BACK").performClick()
        rule.onNodeWithContentDescription("Open system module").performScrollTo().performClick()
        rule.onNodeWithText("Reduced motion").performScrollTo().performClick()
        rule.onNodeWithContentDescription("Motion intensity").assertIsNotEnabled()
    }

    @Test fun unknownCommandShowsRecoverableFailure() {
        rule.onNodeWithText("Enter a local command").performTextInput("invent a successful operation")
        rule.onNodeWithContentDescription("Send command").performClick()
        rule.onNodeWithText("Local command not recognized.", substring = true).assertExists()
        rule.onNodeWithText("RESET").performClick()
        rule.onNodeWithText("Ready for a local command.").assertExists()
    }
}
