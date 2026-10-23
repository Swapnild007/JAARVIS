package com.jaarvis

import android.app.Application
import android.content.*
import android.os.BatteryManager
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import com.jaarvis.core.*
import com.jaarvis.platform.*

data class CommandRecord(val command: String, val response: String, val success: Boolean)

class CommandCenterModel(application: Application) : AndroidViewModel(application) {
    private val router = CommandRouter()
    private val machine = ReactorMachine()
    private val store = PreferenceStore(application)
    private val actions: DeviceActions = AndroidDeviceActions(application, AndroidReminderScheduler(application))
    private val input: SpeechInput = AndroidSpeechInput(application)
    private val output: SpeechOutput = AndroidSpeechOutput(application)
    var state by mutableStateOf(ReactorState.IDLE); private set
    var status by mutableStateOf("Local systems ready. Tap the reactor or type a command."); private set
    var level by mutableFloatStateOf(0f); private set
    var preferences by mutableStateOf(store.load()); private set
    var note by mutableStateOf(store.note()); private set
    var battery by mutableStateOf<Int?>(null); private set
    val history = mutableStateListOf<CommandRecord>()
    val speechAvailable get() = input.available
    val ttsReady get() = output.ready
    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
            val value = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            battery = if (scale > 0 && value >= 0) value * 100 / scale else null
        }
    }
    init { application.registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED)) }
    private fun event(event: ReactorEvent) { state = machine.accept(event) }
    fun updatePreferences(value: Preferences) {
        preferences = value; store.save(value)
        if (!value.sound && state == ReactorState.SPEAKING) { output.stop(); event(ReactorEvent.Complete) }
    }
    fun saveNote(value: String) { note = value; store.saveNote(value) }
    fun clearHistory() { history.clear() }
    fun cancel() {
        input.cancel(); output.stop(); level = 0f; event(ReactorEvent.Reset)
        status = "Ready for a local command."
    }
    fun fail(message: String) { input.cancel(); output.stop(); level = 0f; status = message; event(ReactorEvent.Fail) }
    fun listen() {
        cancel(); event(ReactorEvent.Listen); status = "Listening. Speak one command; tap again to cancel."
        input.start(onText = ::submit, onLevel = { level = it }, onError = ::fail)
    }
    fun submit(text: String) {
        if (text.isBlank()) return
        input.cancel(); output.stop(); level = 0f; event(ReactorEvent.Reset)
        event(ReactorEvent.Recognized); status = "Processing local command…"
        val command = router.parse(text)
        event(ReactorEvent.Execute)
        val result = actions.execute(command)
        status = result.message
        history.add(0, CommandRecord(text.take(500), result.message, result.success))
        while (history.size > 30) history.removeAt(history.lastIndex)
        if (!result.success) { event(ReactorEvent.Fail); return }
        if (preferences.sound && output.speak(result.message,
                onStart = { event(ReactorEvent.Speak) },
                onDone = { event(ReactorEvent.Complete) },
                onError = { status = "${result.message} Spoken feedback failed; check the Android TTS engine."; event(ReactorEvent.Fail) })) {
            // SPEAKING starts only when the real engine reports utterance start.
        } else event(ReactorEvent.Complete)
    }
    fun onBackground() {
        input.cancel(); output.stop(); level = 0f
        if (state != ReactorState.IDLE && state != ReactorState.ERROR) {
            event(ReactorEvent.Reset)
            status = "Paused while JAARVIS was in the background. Ready for a command."
        }
    }
    override fun onCleared() {
        input.close(); output.close(); getApplication<Application>().unregisterReceiver(batteryReceiver)
    }
}
