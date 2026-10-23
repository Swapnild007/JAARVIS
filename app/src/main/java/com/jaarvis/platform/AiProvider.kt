package com.jaarvis.platform

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.net.HttpURLConnection
import java.net.URL
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.spec.GCMParameterSpec
import org.json.JSONArray
import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Credentials remain encrypted at rest with an Android Keystore AES-GCM key. */
class AiSettings(private val context: Context) {
    private val prefs = context.getSharedPreferences("jaarvis_ai", Context.MODE_PRIVATE)
    private val alias = "jaarvis_ai_credential_v1"
    var model: String
        get() = prefs.getString("model", "gemini-2.5-flash") ?: "gemini-2.5-flash"
        set(value) { prefs.edit().putString("model", value.trim().take(100)).apply() }
    val configured get() = prefs.contains("secret")
    fun clear() { prefs.edit().remove("secret").apply() }
    fun saveKey(value: String) {
        require(value.isNotBlank() && value.length <= 4096)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val bytes = cipher.doFinal(value.trim().toByteArray(Charsets.UTF_8))
        val encoded = Base64.encodeToString(cipher.iv + bytes, Base64.NO_WRAP)
        prefs.edit().putString("secret", encoded).apply()
    }
    fun readKey(): String? {
        val raw = prefs.getString("secret", null) ?: return null
        return try {
            val bytes = Base64.decode(raw, Base64.DEFAULT)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, bytes.copyOfRange(0, 12)))
            String(cipher.doFinal(bytes.copyOfRange(12, bytes.size)), Charsets.UTF_8)
        } catch (_: Exception) { null }
    }
    private fun key(): java.security.Key {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        ks.getKey(alias, null)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256).build())
        return generator.generateKey()
    }
}

interface AiProvider { suspend fun reply(prompt: String, model: String, apiKey: String, context: List<Pair<String, String>> = emptyList()): String }

/** Gemini REST API; no third-party HTTP dependency, no key in URL or logs. */
class GeminiProvider : AiProvider {
    override suspend fun reply(prompt: String, model: String, apiKey: String, context: List<Pair<String, String>>): String = withContext(Dispatchers.IO) {
        require(model.matches(Regex("[a-zA-Z0-9._-]{1,100}"))) { "Invalid model name" }
        val url = URL("https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent")
        val connection = url.openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = 12000
            connection.readTimeout = 30000
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            connection.setRequestProperty("x-goog-api-key", apiKey)
            val contents = JSONArray()
            context.takeLast(8).forEach { (role, message) ->
                if (role == "user" || role == "model") contents.put(JSONObject().put("role", role)
                    .put("parts", JSONArray().put(JSONObject().put("text", message.take(3000)))))
            }
            contents.put(JSONObject().put("role", "user")
                .put("parts", JSONArray().put(JSONObject().put("text", prompt.take(12000)))))
            val request = JSONObject().put("contents", contents)
            connection.outputStream.use { it.write(request.toString().toByteArray(Charsets.UTF_8)) }
            val code = connection.responseCode
            if (code !in 200..299) {
                throw IllegalStateException(when (code) {
                    400 -> "Model or request rejected. Check the model setting."
                    401, 403 -> "API key was rejected or this model is not enabled."
                    429 -> "Provider rate limit reached. Try again later."
                    in 500..599 -> "AI provider is temporarily unavailable."
                    else -> "AI request failed (HTTP $code)."
                })
            }
            val response = connection.inputStream.bufferedReader().use { it.readText() }
            val candidates = JSONObject(response).optJSONArray("candidates")
            val parts = candidates?.optJSONObject(0)?.optJSONObject("content")?.optJSONArray("parts")
            val answer = (0 until (parts?.length() ?: 0)).mapNotNull { parts?.optJSONObject(it)?.optString("text") }
                .joinToString("\n").trim()
            if (answer.isBlank()) throw IllegalStateException("The AI provider returned no text.")
            answer.take(16000)
        } finally { connection.disconnect() }
    }
}
