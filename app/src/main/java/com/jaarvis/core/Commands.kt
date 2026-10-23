package com.jaarvis.core

import java.util.Locale

sealed interface Command {
    data object Time : Command
    data object Date : Command
    data object Battery : Command
    data object Settings : Command
    data object YouTube : Command
    data class OpenApp(val app: LocalApp) : Command
    data class Navigate(val destination: String) : Command
    data class Timer(val seconds: Int) : Command
    data class Alarm(val hour: Int, val minute: Int) : Command
    data object AlarmHelp : Command
    data object TimerHelp : Command
    data object Cancel : Command
    data class Unknown(val input: String) : Command
}

enum class LocalApp(val label: String) { CAMERA("Camera"), MAPS("Maps"), CLOCK("Clock"), CALCULATOR("Calculator") }

/** An allowlisted grammar, not a language-model-to-Intent bridge. */
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
            "stop", "cancel", "cancel task" -> Command.Cancel
            "open camera" -> Command.OpenApp(LocalApp.CAMERA)
            "open maps" -> Command.OpenApp(LocalApp.MAPS)
            "open clock" -> Command.OpenApp(LocalApp.CLOCK)
            "open calculator" -> Command.OpenApp(LocalApp.CALCULATOR)
            else -> {
                if (text.startsWith("navigate to ")) {
                    val destination = text.removePrefix("navigate to ").trim()
                    return if (destination.length in 1..200 && !destination.contains(Regex("[\\p{Cntrl}]"))) Command.Navigate(destination) else Command.Unknown(input)
                }
                val timer = Regex("^(?:set |start )?(?:a )?timer(?: for)? (\\d{1,5}) (seconds?|minutes?|hours?)$").matchEntire(text)
                if (timer != null) {
                    val amount = timer.groupValues[1].toLong()
                    val factor = when { timer.groupValues[2].startsWith("hour") -> 3600; timer.groupValues[2].startsWith("minute") -> 60; else -> 1 }
                    val seconds = amount * factor
                    return if (seconds in 1..86400) Command.Timer(seconds.toInt()) else Command.TimerHelp
                }
                if (Regex("^(?:(?:set|start) (?:a )?)?timer\\b").containsMatchIn(text)) return Command.TimerHelp
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

/** A real, bounded one-step plan for R1. No LLM reasoning or autonomous workflows are claimed. */
data class CommandPlan(val command: Command, val description: String)
fun plan(command: Command) = CommandPlan(command, when (command) {
    Command.Time -> "Read the device clock"
    Command.Date -> "Read the device date"
    Command.Battery -> "Read Android battery status"
    Command.Settings -> "Hand off to Android Settings"
    Command.YouTube -> "Request installed YouTube launch"
    is Command.OpenApp -> "Request ${command.app.label} launch"
    is Command.Navigate -> "Open Maps for user review"
    is Command.Alarm -> "Open Clock for alarm review"
    is Command.Timer -> "Open Clock for timer review"
    Command.Cancel -> "Cancel the current JAARVIS operation"
    else -> "Explain the supported local command syntax"
})

enum class ReactorState { IDLE, LISTENING, THINKING, PLANNING, EXECUTING, SPEAKING, COMPLETED, ERROR }
enum class ReactorEvent { Listen, Recognized, Plan, Execute, Speak, Complete, Fail, Reset }
class ReactorMachine {
    var state = ReactorState.IDLE; private set
    fun accept(event: ReactorEvent): ReactorState {
        state = when (event) {
            ReactorEvent.Reset -> ReactorState.IDLE
            ReactorEvent.Fail -> if (state != ReactorState.IDLE) ReactorState.ERROR else state
            ReactorEvent.Listen -> if (state in setOf(ReactorState.IDLE, ReactorState.ERROR, ReactorState.COMPLETED)) ReactorState.LISTENING else state
            ReactorEvent.Recognized -> if (state in setOf(ReactorState.IDLE, ReactorState.LISTENING, ReactorState.ERROR, ReactorState.COMPLETED)) ReactorState.THINKING else state
            ReactorEvent.Plan -> if (state == ReactorState.THINKING) ReactorState.PLANNING else state
            ReactorEvent.Execute -> if (state == ReactorState.PLANNING) ReactorState.EXECUTING else state
            ReactorEvent.Speak -> if (state == ReactorState.EXECUTING) ReactorState.SPEAKING else state
            ReactorEvent.Complete -> if (state in setOf(ReactorState.EXECUTING, ReactorState.SPEAKING)) ReactorState.COMPLETED else state
        }
        return state
    }
}

enum class Outcome { VERIFIED, HANDOFF, FAILED }
data class ActionResult(val message: String, val success: Boolean = true, val outcome: Outcome = if (success) Outcome.VERIFIED else Outcome.FAILED)
interface DeviceActions { fun execute(command: Command): ActionResult }
interface SpeechInput {
    val available: Boolean
    val onDevice: Boolean
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
    fun requestAlarm(hour: Int, minute: Int): ActionResult
    fun requestTimer(seconds: Int): ActionResult
}
