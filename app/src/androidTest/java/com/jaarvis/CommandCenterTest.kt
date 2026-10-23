package com.jaarvis

import android.graphics.Bitmap
import androidx.compose.ui.unit.dp
import java.io.File
import androidx.lifecycle.ViewModelProvider
import com.jaarvis.platform.HistoryStore
import com.jaarvis.platform.PreferenceStore
import com.jaarvis.platform.CommandRecord
import com.jaarvis.core.Outcome
import com.jaarvis.core.ReactorState
import org.junit.Assert.*
import org.junit.Before
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

    @Before fun resetState() {
        rule.runOnIdle {
            val model = ViewModelProvider(rule.activity)[CommandCenterModel::class.java]
            model.cancel(); model.clearHistory(); model.saveNote("")
            model.updatePreferences(model.preferences.copy(sound = false, reducedMotion = false, motion = 0f))
        }
    }

    private fun capture(name: String) {
        val directory = File(rule.activity.getExternalFilesDir(null), "screenshots").apply { mkdirs() }
        File(directory, "$name.png").outputStream().use { stream ->
            check(rule.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, stream))
        }
    }

    @Test fun homeRendersAndCanBeCaptured() {
        rule.onNodeWithContentDescription("Activate voice command").assertExists()
        rule.onNodeWithText("JAARVIS").assertExists()
        // On the default portrait phone, every subsystem must fit above the fixed dock.
        // Enlarged text intentionally uses accessible scrolling instead of shrinking touch targets.
        val metrics = rule.activity.resources.displayMetrics
        if (rule.activity.resources.configuration.fontScale <= 1.25f && metrics.heightPixels / metrics.density >= 780f) {
            val dockTop = rule.onNodeWithTag("command-dock").getUnclippedBoundsInRoot().top
            listOf("intelligence", "operations", "research", "memory", "engineering", "finance", "system", "automations").forEach { name ->
                val node = rule.onNodeWithContentDescription("Open $name module")
                node.assertIsDisplayed().assertHeightIsAtLeast(48.dp)
                check(node.getUnclippedBoundsInRoot().bottom <= dockTop) { "$name overlaps the command dock" }
            }
        }
        capture("home")
    }

    @Test fun everyModuleOpensAndBackReturnsHome() {
        listOf("intelligence", "operations", "research", "memory", "engineering", "finance", "system", "automations").forEach { name ->
            rule.onNodeWithContentDescription("Open $name module").performScrollTo().performClick()
            rule.onNodeWithText(name.uppercase()).assertIsDisplayed()
            capture(name)
            rule.onNodeWithText("‹  BACK").performClick()
            rule.onNodeWithText("ONE CORE. EIGHT CONNECTIONS.").assertExists()
        }
    }

    @Test fun typedBatteryCommandReturnsPlatformReading() {
        rule.onNodeWithText("Enter a local command").performTextInput("battery")
        // IME submission avoids a coordinate tap racing the system keyboard's resize animation.
        // Other tests exercise the GO touch target; this also verifies the keyboard's Send action.
        rule.onNodeWithText("Enter a local command").performImeAction()
        rule.waitUntil(timeoutMillis = 5000) {
            rule.onAllNodesWithText("Battery level is", substring = true).fetchSemanticsNodes().isNotEmpty()
        }
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
        rule.onNodeWithText("Ready for a local command.", substring = true).assertExists()
    }

    @Test fun commandHistoryAndNoteSurviveActivityRecreation() {
        rule.onNodeWithText("Enter a local command").performTextInput("time")
        rule.onNodeWithContentDescription("Send command").performClick()
        rule.waitForIdle()
        rule.runOnIdle {
            val model = ViewModelProvider(rule.activity)[CommandCenterModel::class.java]
            model.saveNote("Persistent R1 note")
            assertEquals(Outcome.VERIFIED, HistoryStore(rule.activity).load().first().outcome)
        }
        rule.activityRule.scenario.recreate()
        rule.runOnIdle {
            val model = ViewModelProvider(rule.activity)[CommandCenterModel::class.java]
            assertEquals("Persistent R1 note", model.note)
            assertEquals("time", model.history.first().command)
        }
        rule.onNodeWithContentDescription("Open intelligence module").performScrollTo().performClick()
        rule.onNodeWithText("Clear command history").performScrollTo().performClick()
        rule.runOnIdle { assertTrue(HistoryStore(rule.activity).load().isEmpty()) }
    }

    @Test fun cancellationBeforeDispatchPreventsExecution() {
        rule.runOnIdle {
            val model = ViewModelProvider(rule.activity)[CommandCenterModel::class.java]
            model.submit("battery")
            model.cancel()
        }
        rule.waitForIdle()
        rule.runOnIdle {
            val model = ViewModelProvider(rule.activity)[CommandCenterModel::class.java]
            assertEquals(ReactorState.IDLE, model.state)
            assertTrue(model.history.isEmpty())
        }
    }

    @Test fun unavailableVoiceOrDeniedPermissionStillAllowsTyping() {
        rule.runOnIdle {
            ViewModelProvider(rule.activity)[CommandCenterModel::class.java]
                .fail("Microphone access was denied. Type a command instead.")
        }
        rule.onNodeWithText("Microphone access was denied.", substring = true).assertExists()
        rule.onNodeWithText("Enter a local command").performTextInput("date")
        rule.onNodeWithContentDescription("Send command").performClick()
        rule.runOnIdle {
            assertEquals(Outcome.VERIFIED, ViewModelProvider(rule.activity)[CommandCenterModel::class.java].history.first().outcome)
        }
        // Exercises the app's error recovery; not an OS microphone permission-dialog test.
    }
}
