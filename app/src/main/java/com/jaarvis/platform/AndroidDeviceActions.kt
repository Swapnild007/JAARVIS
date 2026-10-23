package com.jaarvis.platform

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.provider.AlarmClock
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
            .putExtra(AlarmClock.EXTRA_MESSAGE, "JAARVIS reminder")
            .putExtra(AlarmClock.EXTRA_SKIP_UI, false),
            "Alarm request sent to Clock. Review and save it there.", "No compatible Clock app is available.")
    }
}

internal fun launch(context: Context, intent: Intent, message: String, missing: String): ActionResult = try {
    context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    ActionResult(message)
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
        Command.Settings -> launch(context, Intent(Settings.ACTION_SETTINGS), "Android Settings opened.", "Android Settings is unavailable.")
        Command.YouTube -> {
            val intent = context.packageManager.getLaunchIntentForPackage("com.google.android.youtube")
            if (intent == null) ActionResult("YouTube is not installed or is disabled on this device.", false)
            else launch(context, intent, "YouTube opened.", "YouTube could not be opened.")
        }
        is Command.Alarm -> reminders.requestAlarm(command.hour, command.minute)
        Command.AlarmHelp -> ActionResult("Use ‘alarm 07:30’ or ‘set alarm at 7:30 am’. Review and save it in Clock.", false)
        is Command.Unknown -> ActionResult("Local command not recognized. Try time, date, battery, open settings, open YouTube, or alarm 07:30. Cloud intelligence is not connected.", false)
    }
}
