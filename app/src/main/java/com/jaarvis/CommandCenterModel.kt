package com.jaarvis

import android.app.Application
import android.content.*
import android.os.BatteryManager
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jaarvis.core.*
import com.jaarvis.platform.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield

class CommandCenterModel(application: Application) : AndroidViewModel(application) {
    private val router = CommandRouter()
    private val machine = ReactorMachine()
    private val store = PreferenceStore(application)
    private val historyStore = HistoryStore(application)
    private val actions: DeviceActions = AndroidDeviceActions(application, AndroidReminderScheduler(application))
    private val input: SpeechInput = AndroidSpeechInput(application)
    var ttsReady by mutableStateOf(false); private set
    private val output: SpeechOutput = AndroidSpeechOutput(application) { ttsReady = it }
    private var operation: Job? = null
    private var generation = 0L
    var state by mutableStateOf(ReactorState.IDLE); private set
    var status by mutableStateOf("Local systems ready. Tap the reactor or type a command."); private set
    var level by mutableFloatStateOf(0f); private set
    var activePlan by mutableStateOf(""); private set
    var preferences by mutableStateOf(store.load()); private set
    var note by mutableStateOf(store.note()); private set
    var battery by mutableStateOf<Int?>(null); private set
    val history = mutableStateListOf<CommandRecord>().apply { addAll(historyStore.load()) }
    val speechAvailable get() = input.available
    val onDeviceSpeech get() = input.onDevice
    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
            val value = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            battery = if (scale > 0 && value >= 0) (value * 100 / scale).coerceIn(0, 100) else null
        }
    }
    init { application.registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED)) }
    private fun event(event: ReactorEvent) { state = machine.accept(event) }
    fun updatePreferences(value: Preferences) {
        preferences = value; store.save(value)
        if (!value.sound) { output.stop(); if (state == ReactorState.SPEAKING || state == ReactorState.EXECUTING) event(ReactorEvent.Complete) }
    }
    fun saveNote(value: String) { note = value; store.saveNote(value) }
    fun clearHistory() { history.clear(); historyStore.save(emptyList()) }
    fun cancel() {
        generation++; operation?.cancel(); operation = null
        input.cancel(); output.stop(); level = 0f; activePlan = ""; event(ReactorEvent.Reset)
        status = "Ready for a local command. Dispatched Clock or app requests must be cancelled in that app."
    }
    fun fail(message: String) {
        cancel(); event(ReactorEvent.Recognized); event(ReactorEvent.Fail); status = message
    }
    fun listen() {
        cancel(); val token = generation
        event(ReactorEvent.Listen)
        status = if (onDeviceSpeech) "On-device recognition. Speak one command; tap again to cancel."
            else "Listening through Android's speech service, which may use its network. Tap to cancel."
        input.start(onText = { if (token == generation) submit(it) },
            onLevel = { if (token == generation) level = it }, onError = { if (token == generation) fail(it) })
    }
    fun submit(text: String) {
        if (text.isBlank()) return
        cancel()
        if (router.parse(text) == Command.Cancel) return
        val token = generation
        operation = viewModelScope.launch {
            event(ReactorEvent.Recognized); status = "Interpreting the local command…"
            yield(); ensureActive()
            val planned = plan(router.parse(text))
            activePlan = planned.description; event(ReactorEvent.Plan)
            status = planned.description
            yield(); ensureActive()
            event(ReactorEvent.Execute)
            val result = try { actions.execute(planned.command) }
                catch (_: RuntimeException) { ActionResult("Device action failed. No completion could be verified.", false) }
            if (token != generation) return@launch
            status = result.message
            history.add(0, CommandRecord(text.take(500), result.message, result.success, result.outcome,
                plan = planned.description, stages = listOf("THINKING", "PLANNING", "EXECUTING", result.outcome.name)))
            while (history.size > 30) history.removeAt(history.lastIndex)
            historyStore.save(history.toList())
            if (!result.success) { event(ReactorEvent.Fail); return@launch }
            if (preferences.sound) {
                val accepted = output.speak(result.message,
                    onStart = { if (token == generation) event(ReactorEvent.Speak) },
                    onDone = { if (token == generation) event(ReactorEvent.Complete) },
                    onError = { if (token == generation) { status = "${result.message} Spoken feedback failed; check the Android TTS engine."; event(ReactorEvent.Fail) } })
                if (!accepted) { status = "${result.message} Spoken feedback unavailable; text result retained."; event(ReactorEvent.Complete) }
            } else event(ReactorEvent.Complete)
        }
    }
    fun onBackground() {
        // Configuration changes keep the ViewModel; genuine backgrounding stops microphone and speech.
        val lastStatus = status
        val finished = state in setOf(ReactorState.COMPLETED, ReactorState.ERROR, ReactorState.EXECUTING, ReactorState.SPEAKING)
        cancel()
        status = if (finished) lastStatus else "Paused in background. Ready for a command."
    }
    override fun onCleared() {
        generation++; operation?.cancel(); input.close(); output.close()
        getApplication<Application>().unregisterReceiver(batteryReceiver)
    }
}
