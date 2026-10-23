package com.jaarvis.platform

import android.content.Context
import com.jaarvis.core.Outcome
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class CommandRecord(
    val command: String, val response: String, val success: Boolean,
    val outcome: Outcome = if (success) Outcome.VERIFIED else Outcome.FAILED,
    val timestamp: Long = System.currentTimeMillis(), val id: String = UUID.randomUUID().toString(),
    val plan: String = "", val stages: List<String> = emptyList()
)

/** Bounded app-private history. Not a durable preference memory, and never sent to a provider. */
class HistoryStore(context: Context) {
    private val prefs = context.getSharedPreferences("jaarvis_history", Context.MODE_PRIVATE)
    fun load(): List<CommandRecord> = try {
        val list = JSONArray(prefs.getString("records", "[]"))
        (0 until minOf(list.length(), 30)).map { index ->
            val row = list.getJSONObject(index)
            val stages = row.optJSONArray("stages") ?: JSONArray()
            CommandRecord(row.getString("command").take(500), row.getString("response").take(3000), row.getBoolean("success"),
                Outcome.valueOf(row.getString("outcome")), row.getLong("timestamp"), row.getString("id"),
                row.optString("plan"), (0 until stages.length()).map { stages.getString(it) })
        }
    } catch (_: Exception) { emptyList() }
    fun save(history: List<CommandRecord>) {
        val rows = JSONArray()
        history.take(30).forEach { r -> rows.put(JSONObject().put("id", r.id).put("timestamp", r.timestamp)
            .put("command", r.command).put("response", r.response).put("success", r.success)
            .put("outcome", r.outcome.name).put("plan", r.plan).put("stages", JSONArray(r.stages))) }
        prefs.edit().putString("records", rows.toString()).apply()
    }
}
