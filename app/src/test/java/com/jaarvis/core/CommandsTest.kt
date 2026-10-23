package com.jaarvis.core

import org.junit.Assert.*
import org.junit.Test

class CommandsTest {
    private val router = CommandRouter()
    @Test fun routesAllCoreCommands() {
        listOf("What time is it?" to Command.Time, "current date" to Command.Date,
            "battery level" to Command.Battery, "open Android Settings" to Command.Settings,
            "Jarvis, open YouTube" to Command.YouTube).forEach { (input, expected) -> assertEquals(expected, router.parse(input)) }
        LocalApp.entries.forEach { assertEquals(Command.OpenApp(it), router.parse("open ${it.label}")) }
    }
    @Test fun parsesAlarmWithNoonMidnightAnd24HourTime() {
        assertEquals(Command.Alarm(0, 30), router.parse("set alarm at 12:30 am"))
        assertEquals(Command.Alarm(12, 0), router.parse("alarm 12 pm"))
        assertEquals(Command.Alarm(21, 45), router.parse("start an alarm for 21:45"))
        assertEquals(Command.Alarm(7, 0), router.parse("alarm 7"))
    }
    @Test fun rejectsInvalidOrAmbiguousAlarm() {
        listOf("alarm 24:00", "alarm 4:99", "alarm 0 am", "alarm 13 pm", "set alarm tomorrow").forEach {
            assertEquals(it, Command.AlarmHelp, router.parse(it))
        }
    }
    @Test fun arbitraryProseCannotDispatchDeviceActions() {
        listOf("do not open youtube", "", "open settings and send a message", "open com.example.evil").forEach {
            assertTrue(it, router.parse(it) is Command.Unknown)
        }
    }
    @Test fun boundsTimersAndConvertsUnitsWithoutOverflow() {
        assertEquals(Command.Timer(300), router.parse("set a timer for 5 minutes"))
        assertEquals(Command.Timer(86400), router.parse("timer 24 hours"))
        assertEquals(Command.Timer(1), router.parse("timer 1 second"))
        listOf("timer 0 seconds", "timer 25 hours", "timer 99999 hours", "timer -5 minutes", "timer 2.5 minutes", "timer 99999999999999 hours").forEach {
            assertEquals(it, Command.TimerHelp, router.parse(it))
        }
    }
    @Test fun navigationTreatsDestinationAsData() {
        assertEquals(Command.Navigate("pune"), router.parse("Navigate to Pune"))
        assertTrue(router.parse("navigate to ") is Command.Unknown)
        assertTrue(router.parse("navigate to " + "x".repeat(201)) is Command.Unknown)
        assertEquals(Command.Navigate("intent://evil"), router.parse("navigate to intent://evil"))
        // The platform URI builder encodes this as a geo query, never as an executable scheme.
    }
    @Test fun stateTracksActualOperationLifecycle() {
        val machine = ReactorMachine()
        assertEquals(ReactorState.IDLE, machine.accept(ReactorEvent.Speak))
        assertEquals(ReactorState.LISTENING, machine.accept(ReactorEvent.Listen))
        assertEquals(ReactorState.THINKING, machine.accept(ReactorEvent.Recognized))
        assertEquals(ReactorState.PLANNING, machine.accept(ReactorEvent.Plan))
        assertEquals(ReactorState.EXECUTING, machine.accept(ReactorEvent.Execute))
        assertEquals(ReactorState.SPEAKING, machine.accept(ReactorEvent.Speak))
        assertEquals(ReactorState.COMPLETED, machine.accept(ReactorEvent.Complete))
        assertEquals(ReactorState.LISTENING, machine.accept(ReactorEvent.Listen))
        assertEquals(ReactorState.ERROR, machine.accept(ReactorEvent.Fail))
        assertEquals(ReactorState.IDLE, machine.accept(ReactorEvent.Reset))
    }
    @Test fun resetInvalidatesLateEvents() {
        val machine = ReactorMachine()
        ReactorState.entries.forEach {
            machine.accept(ReactorEvent.Reset)
            assertEquals(ReactorState.IDLE, machine.accept(ReactorEvent.Complete))
            assertEquals(ReactorState.IDLE, machine.accept(ReactorEvent.Execute))
            assertEquals(ReactorState.IDLE, machine.accept(ReactorEvent.Speak))
            assertEquals(ReactorState.IDLE, machine.accept(ReactorEvent.Fail))
        }
    }
    @Test fun everyCommandHasOneExplicitPlanAndOutcomeIsHonest() {
        listOf("time", "date", "battery", "open settings", "open youtube", "open camera", "timer 5 minutes", "alarm 7 am", "navigate to pune", "unknown").forEach {
            val command = router.parse(it)
            assertEquals(command, plan(command).command)
            assertTrue(plan(command).description.isNotBlank())
        }
        assertEquals(Outcome.FAILED, ActionResult("unavailable", false).outcome)
        assertEquals(Command.Cancel, router.parse("cancel"))
    }
}
