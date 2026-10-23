package com.jaarvis

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.provider.AlarmClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.jaarvis.core.*
import com.jaarvis.platform.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DeviceActionsTest {
    private class RecordingContext(base: Context, val missing: Boolean = false) : ContextWrapper(base) {
        var last: Intent? = null
        override fun startActivity(intent: Intent) {
            if (missing) throw ActivityNotFoundException("Test: no matching activity")
            last = intent
        }
    }
    private fun context(missing: Boolean = false) = RecordingContext(InstrumentationRegistry.getInstrumentation().targetContext, missing)

    @Test fun alarmsAndTimersRequireVisibleClockReview() {
        val context = context()
        val scheduler = AndroidReminderScheduler(context)
        val result = scheduler.requestAlarm(7, 30)
        assertEquals(Outcome.HANDOFF, result.outcome)
        assertEquals(AlarmClock.ACTION_SET_ALARM, context.last!!.action)
        assertFalse(context.last!!.getBooleanExtra(AlarmClock.EXTRA_SKIP_UI, true))
        assertEquals(7, context.last!!.getIntExtra(AlarmClock.EXTRA_HOUR, -1))
        assertEquals(Outcome.HANDOFF, scheduler.requestTimer(300).outcome)
        assertEquals(300, context.last!!.getIntExtra(AlarmClock.EXTRA_LENGTH, -1))
        assertFalse(context.last!!.getBooleanExtra(AlarmClock.EXTRA_SKIP_UI, true))
        context.last = null
        assertFalse(scheduler.requestTimer(86401).success)
        assertFalse(scheduler.requestAlarm(25, 0).success)
        assertNull(context.last)
    }
    @Test fun untrustedDestinationCannotChangeTheIntentScheme() {
        val context = context()
        val actions = AndroidDeviceActions(context, AndroidReminderScheduler(context))
        val destination = "intent://evil#Intent;scheme=tel;end &x=1"
        assertEquals(Outcome.HANDOFF, actions.execute(Command.Navigate(destination)).outcome)
        assertEquals("geo", context.last!!.data!!.scheme)
        assertEquals(destination, android.net.Uri.decode(context.last!!.data!!.encodedSchemeSpecificPart.substringAfter("?q=")))
        assertTrue(context.last!!.flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0)
    }
    @Test fun unavailableAppsReturnFailureInsteadOfSuccess() {
        val context = context(missing = true)
        val actions = AndroidDeviceActions(context, AndroidReminderScheduler(context))
        LocalApp.entries.forEach { assertEquals(Outcome.FAILED, actions.execute(Command.OpenApp(it)).outcome) }
        assertEquals(Outcome.FAILED, actions.execute(Command.Timer(10)).outcome)
    }
    @Test fun notesPreferencesAndHistoryReadBackFromStorage() {
        val context = context()
        val prefs = PreferenceStore(context)
        prefs.saveNote("Persistent note")
        assertEquals("Persistent note", PreferenceStore(context).note())
        val old = prefs.load()
        prefs.save(old.copy(reducedMotion = true))
        assertTrue(PreferenceStore(context).load().reducedMotion)
        prefs.save(old)
        val store = HistoryStore(context)
        store.save(listOf(CommandRecord("battery", "50 percent", true, Outcome.VERIFIED)))
        assertEquals("battery", HistoryStore(context).load().single().command)
        store.save(emptyList())
        assertTrue(HistoryStore(context).load().isEmpty())
        prefs.saveNote("")
    }
}
