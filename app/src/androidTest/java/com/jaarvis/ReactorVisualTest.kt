package com.jaarvis

import android.graphics.Bitmap
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.jaarvis.core.ReactorState
import com.jaarvis.ui.*
import org.junit.Rule
import org.junit.Test
import java.io.File

/** Rendering fixtures only: these screenshots are NOT evidence of actual microphone/TTS execution. */
class ReactorVisualTest {
    @get:Rule val rule = createComposeRule()
    @Test fun everyRealStateHasAReadableReactorAndEightWorkingSubsystems() {
        val state = mutableStateOf(ReactorState.IDLE)
        var opened: Module? = null
        rule.setContent {
            MaterialTheme {
                Surface(Modifier.fillMaxSize(), color = Ink) {
                    ReactorMap(state.value, .7f, 0f, false, {}, { opened = it })
                }
            }
        }
        val directory = File(InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null), "screenshots").apply { mkdirs() }
        ReactorState.entries.forEach { value ->
            rule.runOnIdle { state.value = value }
            rule.onNodeWithText(value.name).assertIsDisplayed()
            File(directory, "fixture-${value.name.lowercase()}.png").outputStream().use {
                check(rule.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it))
            }
        }
        Module.entries.forEach { module ->
            rule.onNodeWithContentDescription("Open ${module.name.lowercase()} module").performClick()
            rule.runOnIdle { check(opened == module) }
        }
    }

    @Test fun orbitalAnimationChangesPixelsWithoutChangingTheOperationState() {
        rule.mainClock.autoAdvance = false
        rule.setContent { MaterialTheme { Surface(Modifier.fillMaxSize(), color = Ink) {
            ReactorMap(ReactorState.THINKING, 0f, 1f, true, {}, {})
        } } }
        rule.mainClock.advanceTimeByFrame()
        val first = rule.onRoot().captureToImage().asAndroidBitmap()
        rule.mainClock.advanceTimeBy(1800)
        val second = rule.onRoot().captureToImage().asAndroidBitmap()
        check(!first.sameAs(second)) { "Orbital motion did not produce a new frame" }
        rule.onNodeWithText("THINKING").assertExists()
    }

    @Test fun disabledMotionKeepsTheReactorFrameStable() {
        rule.mainClock.autoAdvance = false
        rule.setContent { MaterialTheme { Surface(Modifier.fillMaxSize(), color = Ink) {
            ReactorMap(ReactorState.IDLE, 0f, 0f, false, {}, {})
        } } }
        rule.mainClock.advanceTimeByFrame()
        val first = rule.onRoot().captureToImage().asAndroidBitmap()
        rule.mainClock.advanceTimeBy(1800)
        val second = rule.onRoot().captureToImage().asAndroidBitmap()
        check(first.sameAs(second)) { "Reduced-motion frame changed without an event" }
    }
}
