package com.jaarvis.platform

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.BatteryManager
import android.provider.AlarmClock
import android.provider.MediaStore
import android.provider.Settings
import com.jaarvis.core.*
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class AndroidReminderScheduler(private val context: Context) : ReminderScheduler {
    override fun requestAlarm(hour: Int, minute: Int): ActionResult {
        if (hour !in 0..23 || minute !in 0..59) return ActionResult("Enter a valid hour and minute.", false)
        return launch(context, Intent(AlarmClock.ACTION_SET_ALARM)
            .putExtra(AlarmClock.EXTRA_HOUR, hour).putExtra(AlarmClock.EXTRA_MINUTES, minute)
            .putExtra(AlarmClock.EXTRA_MESSAGE, "JAARVIS reminder").putExtra(AlarmClock.EXTRA_SKIP_UI, false),
            "Alarm request sent to Clock. Review and save it there.", "No compatible Clock app is available.")
    }
    override fun requestTimer(seconds: Int): ActionResult {
        if (seconds !in 1..86400) return ActionResult("Timer must be between 1 second and 24 hours.", false)
        return launch(context, Intent(AlarmClock.ACTION_SET_TIMER).putExtra(AlarmClock.EXTRA_LENGTH, seconds)
            .putExtra(AlarmClock.EXTRA_MESSAGE, "JAARVIS timer").putExtra(AlarmClock.EXTRA_SKIP_UI, false),
            "Timer request sent to Clock. Review and start it there.", "No compatible timer app is available.")
    }
}

/** Dispatch is evidence of a handoff only. It does not verify the receiving app's UI or saved state. */
internal fun launch(context: Context, intent: Intent, message: String, missing: String): ActionResult = try {
    context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    ActionResult(message, outcome = Outcome.HANDOFF)
} catch (_: ActivityNotFoundException) { ActionResult(missing, false) }
  catch (_: SecurityException) { ActionResult("Android blocked this action. Check app permissions.", false) }

fun batteryPercent(context: Context): Int? {
    val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)) ?: return null
    val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
    val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
    return if (level >= 0 && scale > 0) (level * 100 / scale).coerceIn(0, 100) else null
}

class AndroidDeviceActions(private val context: Context, private val reminders: ReminderScheduler) : DeviceActions {
    override fun execute(command: Command): ActionResult = when (command) {
        Command.Time -> ActionResult("It is ${ZonedDateTime.now().format(DateTimeFormatter.ofPattern("h:mm a z", Locale.getDefault()))}.")
        Command.Date -> ActionResult(ZonedDateTime.now().format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.getDefault())))
        Command.Battery -> batteryPercent(context)?.let { ActionResult("Battery level is $it percent.") }
            ?: ActionResult("Android did not report a battery level.", false)
        Command.Settings -> launch(context, Intent(Settings.ACTION_SETTINGS), "Android Settings launch requested.", "Android Settings is unavailable.")
        Command.YouTube -> {
            val intent = context.packageManager.getLaunchIntentForPackage("com.google.android.youtube")
            if (intent == null) ActionResult("YouTube is not installed or is disabled on this device.", false)
            else launch(context, intent, "YouTube launch requested.", "YouTube could not be opened.")
        }
        is Command.OpenApp -> {
            val intent = when (command.app) {
                LocalApp.CAMERA -> Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)
                LocalApp.MAPS -> Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q="))
                LocalApp.CLOCK -> Intent(AlarmClock.ACTION_SHOW_ALARMS)
                LocalApp.CALCULATOR -> Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_CALCULATOR)
            }
            launch(context, intent, "${command.app.label} launch requested.", "No compatible ${command.app.label.lowercase()} app is available.")
        }
        is Command.Navigate -> if (command.destination.length !in 1..200) ActionResult("Enter a destination up to 200 characters.", false)
            else launch(context, Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=${Uri.encode(command.destination)}")),
                "Destination handed to Maps. Choose directions and review the route there.", "No map application is installed.")
        is Command.Alarm -> reminders.requestAlarm(command.hour, command.minute)
        is Command.Timer -> reminders.requestTimer(command.seconds)
        Command.Cancel -> ActionResult("No device action dispatched.")
        Command.AlarmHelp -> ActionResult("Use ‘alarm 07:30’ or ‘set alarm at 7:30 am’. Review and save it in Clock.", false)
        Command.TimerHelp -> ActionResult("Use ‘timer 5 minutes’. Maximum 24 hours. Review and start it in Clock.", false)
        is Command.Unknown -> ActionResult("Local command not recognized. Try time, date, battery, open settings, open camera, open maps, open clock, open calculator, open YouTube, timer 5 minutes, or alarm 07:30. Cloud intelligence is not connected.", false)
    }
}
