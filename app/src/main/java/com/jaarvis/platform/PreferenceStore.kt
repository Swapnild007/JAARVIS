package com.jaarvis.platform

import android.content.Context

data class Preferences(val sound: Boolean = true, val motion: Float = 0.65f, val reducedMotion: Boolean = false, val theme: String = "Midnight")
class PreferenceStore(context: Context) {
    private val store = context.getSharedPreferences("jaarvis_local", Context.MODE_PRIVATE)
    fun load() = Preferences(store.getBoolean("sound", true), store.getFloat("motion", .65f).coerceIn(0f, 1f),
        store.getBoolean("reduced", false), store.getString("theme", "Midnight") ?: "Midnight")
    fun save(p: Preferences) { store.edit().putBoolean("sound", p.sound).putFloat("motion", p.motion)
        .putBoolean("reduced", p.reducedMotion).putString("theme", p.theme).apply() }
    fun note(): String = store.getString("note", "") ?: ""
    fun saveNote(text: String) { store.edit().putString("note", text).apply() }
}
