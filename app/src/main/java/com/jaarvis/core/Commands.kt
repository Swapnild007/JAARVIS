package com.jaarvis.core

import java.util.Locale

sealed interface Command {
    data object Time : Command
    data object Date : Command
    data object Battery : Command
    data object Settings : Command
    data object YouTube : Command
    data class Alarm(val hour: Int, val minute: Int) : Command
    data object AlarmHelp : Command
    data class Unknown(val input: String) : Command
}

/** Deliberately bounded local grammar: never infer a device action from arbitrary prose. */
class CommandRouter {
    fun parse(input: String): Command {
        val text = input.trim().lowercase(Locale.ROOT).replace(Regex("[?!.,]+$"), "")
            .replace(Regex("^(hey |ok )?(jaarvis|jarvis)[, ]+"), "").trim()
        return when (text) {
            "time", "current time", "what time is it", "what is the time", "tell me the time" -> Command.Time
            "date", "current date", "today's date", "what is today's date", "what is the date" -> Command.Date
            "battery", "battery level", "what is my battery level", "check battery" -> Command.Battery
            "settings", "open settings", "open android settings" -> Command.Settings
            "youtube", "open youtube" -> Command.YouTube
            else -> {
                val match = Regex("^(?:set |start )?(?:an? )?alarm(?: at| for)? (\\d{1,2})(?::(\\d{2}))?\\s*(am|pm)?$").matchEntire(text)
                if (match != null) {
                    var hour = match.groupValues[1].toInt()
                    val minute = match.groupValues[2].ifEmpty { "0" }.toInt()
                    val period = match.groupValues[3]
                    if (minute !in 0..59 || (period.isNotEmpty() && hour !in 1..12) || (period.isEmpty() && hour !in 0..23)) Command.AlarmHelp
                    else {
                        if (period.isNotEmpty()) hour = hour % 12 + if (period == "pm") 12 else 0
                        Command.Alarm(hour, minute)
                    }
                } else if (text.startsWith("alarm") || text.startsWith("set alarm") || text.startsWith("start alarm")) Command.AlarmHelp
                else Command.Unknown(input)
            }
        }
    }
}

enum class ReactorState { IDLE, LISTENING, PROCESSING, SPEAKING, EXECUTING, ERROR }
enum class ReactorEvent { Listen, Recognized, Execute, Speak, Complete, Fail, Reset }

/** Invalid late callbacks cannot, for example, turn an idle reactor into a speaking one. */
class ReactorMachine {
    var state = ReactorState.IDLE
        private set
    fun accept(event: ReactorEvent): ReactorState {
        state = when (event) {
            ReactorEvent.Reset -> ReactorState.IDLE
            ReactorEvent.Fail -> ReactorState.ERROR
            ReactorEvent.Listen -> if (state in setOf(ReactorState.IDLE, ReactorState.ERROR)) ReactorState.LISTENING else state
            ReactorEvent.Recognized -> if (state in setOf(ReactorState.IDLE, ReactorState.LISTENING, ReactorState.ERROR)) ReactorState.PROCESSING else state
            ReactorEvent.Execute -> if (state == ReactorState.PROCESSING) ReactorState.EXECUTING else state
            ReactorEvent.Speak -> if (state in setOf(ReactorState.PROCESSING, ReactorState.EXECUTING)) ReactorState.SPEAKING else state
            ReactorEvent.Complete -> if (state in setOf(ReactorState.PROCESSING, ReactorState.EXECUTING, ReactorState.SPEAKING)) ReactorState.IDLE else state
        }
        return state
    }
}

data class ActionResult(val message: String, val success: Boolean = true)
interface DeviceActions { fun execute(command: Command): ActionResult }
interface SpeechInput {
    val available: Boolean
    fun start(onText: (String) -> Unit, onLevel: (Float) -> Unit, onError: (String) -> Unit)
    fun cancel()
    fun close()
}
interface SpeechOutput {
    val ready: Boolean
    fun speak(text: String, onStart: () -> Unit, onDone: () -> Unit, onError: () -> Unit): Boolean
    fun stop()
    fun close()
}
interface ReminderScheduler {
    /** Opens the system Clock confirmation UI; does not assert the user saved it. */
    fun requestAlarm(hour: Int, minute: Int): ActionResult
}
