package com.jaarvis.core

import org.junit.Assert.*
import org.junit.Test

class CommandsTest {
    private val router = CommandRouter()
    @Test fun routesAllCoreCommands() {
        listOf("What time is it?" to Command.Time, "current date" to Command.Date,
            "battery level" to Command.Battery, "open Android Settings" to Command.Settings,
            "Jarvis, open YouTube" to Command.YouTube).forEach { (input, expected) -> assertEquals(expected, router.parse(input)) }
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
        assertTrue(router.parse("do not open youtube") is Command.Unknown)
        assertTrue(router.parse("") is Command.Unknown)
    }
    @Test fun stateTracksActualOperationLifecycle() {
        val machine = ReactorMachine()
        assertEquals(ReactorState.IDLE, machine.accept(ReactorEvent.Speak))
        assertEquals(ReactorState.LISTENING, machine.accept(ReactorEvent.Listen))
        assertEquals(ReactorState.PROCESSING, machine.accept(ReactorEvent.Recognized))
        assertEquals(ReactorState.EXECUTING, machine.accept(ReactorEvent.Execute))
        assertEquals(ReactorState.SPEAKING, machine.accept(ReactorEvent.Speak))
        assertEquals(ReactorState.IDLE, machine.accept(ReactorEvent.Complete))
        assertEquals(ReactorState.ERROR, machine.accept(ReactorEvent.Fail))
        assertEquals(ReactorState.IDLE, machine.accept(ReactorEvent.Reset))
    }
    @Test fun resetInvalidatesLateCompletion() {
        val machine = ReactorMachine()
        machine.accept(ReactorEvent.Listen)
        machine.accept(ReactorEvent.Reset)
        assertEquals(ReactorState.IDLE, machine.accept(ReactorEvent.Complete))
        assertEquals(ReactorState.IDLE, machine.accept(ReactorEvent.Execute))
    }
}
