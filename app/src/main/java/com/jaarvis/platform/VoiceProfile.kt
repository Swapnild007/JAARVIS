package com.jaarvis.platform

import android.content.Context

/** Voice names are engine-defined; preview is necessary to judge timbre. */
class VoiceProfile(context: Context) {
    private val prefs = context.getSharedPreferences("jaarvis_voice", Context.MODE_PRIVATE)
    var voiceName: String
        get() = prefs.getString("voice", "") ?: ""
        set(value) { prefs.edit().putString("voice", value).apply() }
    var rate: Float
        get() = prefs.getFloat("rate", .92f).coerceIn(.6f, 1.4f)
        set(value) { prefs.edit().putFloat("rate", value.coerceIn(.6f, 1.4f)).apply() }
    var pitch: Float
        get() = prefs.getFloat("pitch", .85f).coerceIn(.6f, 1.3f)
        set(value) { prefs.edit().putFloat("pitch", value.coerceIn(.6f, 1.3f)).apply() }
}
