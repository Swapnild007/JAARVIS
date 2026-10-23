package com.jaarvis.platform

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.jaarvis.core.SpeechInput
import com.jaarvis.core.SpeechOutput
import java.util.Locale
import java.util.UUID

class AndroidSpeechInput(private val context: Context) : SpeechInput {
    private var recognizer: SpeechRecognizer? = null
    private var generation = 0
    private val handler = Handler(Looper.getMainLooper())
    private var timeout: Runnable? = null
    override val onDevice get() = Build.VERSION.SDK_INT >= 31 && SpeechRecognizer.isOnDeviceRecognitionAvailable(context)
    override val available get() = onDevice || SpeechRecognizer.isRecognitionAvailable(context)
    override fun start(onText: (String) -> Unit, onLevel: (Float) -> Unit, onError: (String) -> Unit) {
        cancel()
        if (!available) { onError("No speech recognition service is installed. Use the keyboard."); return }
        val token = generation
        try {
            val engine = if (Build.VERSION.SDK_INT >= 31 && onDevice) SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
                else SpeechRecognizer.createSpeechRecognizer(context)
            recognizer = engine
            engine.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) { if (token == generation) onLevel(((rmsdB + 2f) / 12f).coerceIn(0f, 1f)) }
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() { if (token == generation) onLevel(0f) }
                override fun onError(error: Int) {
                    if (token != generation) return
                    val message = when (error) {
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission is required. Enable it in Android app settings or type instead."
                        SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech recognized. Tap the reactor to retry, or type a command."
                        SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "The recognition service needs a connection. Type a command instead."
                        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Speech service is busy. Tap the reactor to retry."
                        else -> "Speech recognition failed (code $error). Tap the reactor to retry or use the keyboard."
                    }
                    cancel()
                    onError(message)
                }
                override fun onResults(results: Bundle?) {
                    if (token != generation) return
                    val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                    cancel()
                    if (text.isNullOrBlank()) onError("No speech recognized. Try again.") else onText(text)
                }
                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
            timeout = Runnable { if (token == generation) { cancel(); onError("Listening timed out. Tap to retry or type a command.") } }.also { handler.postDelayed(it, 20000) }
            engine.startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
                .putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1))
        } catch (_: RuntimeException) { cancel(); onError("Speech service could not start. Type a command instead.") }
    }
    override fun cancel() {
        generation++
        timeout?.let(handler::removeCallbacks); timeout = null
        val old = recognizer; recognizer = null
        try { old?.cancel() } finally { old?.destroy() }
    }
    override fun close() = cancel()
}

class AndroidSpeechOutput(context: Context, private val readiness: (Boolean) -> Unit = {}) : SpeechOutput {
    private val handler = Handler(Looper.getMainLooper())
    private var engine: TextToSpeech? = null
    private var initialized = false
    private var activeId: String? = null
    private var timeout: Runnable? = null
    override val ready get() = initialized
    init {
        engine = TextToSpeech(context) { status ->
            initialized = status == TextToSpeech.SUCCESS
            if (initialized) {
                val result = engine?.setLanguage(Locale.getDefault())
                initialized = result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED
            }
            readiness(initialized)
        }
    }
    override fun speak(text: String, onStart: () -> Unit, onDone: () -> Unit, onError: () -> Unit): Boolean {
        if (!ready) return false
        stop()
        val id = UUID.randomUUID().toString()
        activeId = id
        engine?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) { handler.post { if (activeId == utteranceId) onStart() } }
            override fun onDone(utteranceId: String?) { handler.post { if (activeId == utteranceId) { activeId = null; timeout?.let(handler::removeCallbacks); timeout = null; onDone() } } }
            @Deprecated("Platform callback")
            override fun onError(utteranceId: String?) { handler.post { if (activeId == utteranceId) { activeId = null; timeout?.let(handler::removeCallbacks); timeout = null; onError() } } }
        })
        val accepted = engine?.speak(text, TextToSpeech.QUEUE_FLUSH, null, id) == TextToSpeech.SUCCESS
        if (!accepted) activeId = null
        else timeout = Runnable { if (activeId == id) { stop(); onError() } }.also { handler.postDelayed(it, 45000) }
        return accepted
    }
    override fun stop() { activeId = null; timeout?.let(handler::removeCallbacks); timeout = null; engine?.stop() }
    override fun close() { stop(); initialized = false; engine?.shutdown(); engine = null }
}
