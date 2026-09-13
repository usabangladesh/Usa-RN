package com.example.gemini

import android.content.Context
import android.content.SharedPreferences
import com.example.BuildConfig

object ApiKeyManager {
    private const val PREFS_NAME = "rashed_api_config"
    private const val KEY_CUSTOM_API_KEY = "custom_gemini_api_key"

    fun init(context: Context) {
        val saved = getCustomApiKey(context)
        if (!saved.isNullOrBlank()) {
            GeminiLiveClient.runtimeApiKeyOverride = saved
        }
    }

    fun getCustomApiKey(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_CUSTOM_API_KEY, null)?.takeIf { it.isNotBlank() }
    }

    fun getActiveApiKey(context: Context): String {
        val custom = getCustomApiKey(context)
        if (!custom.isNullOrBlank()) {
            return custom
        }
        val buildKey = BuildConfig.GEMINI_API_KEY.trim()
        if (buildKey.isNotBlank() && buildKey != "DEFAULT_API_KEY" && buildKey != "MY_GEMINI_API_KEY") {
            return buildKey
        }
        return ""
    }

    fun saveCustomApiKey(context: Context, key: String) {
        val trimmed = key.trim()
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (trimmed.isBlank()) {
            prefs.edit().remove(KEY_CUSTOM_API_KEY).apply()
            GeminiLiveClient.runtimeApiKeyOverride = null
        } else {
            prefs.edit().putString(KEY_CUSTOM_API_KEY, trimmed).apply()
            GeminiLiveClient.runtimeApiKeyOverride = trimmed
        }
    }

    fun clearCustomApiKey(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_CUSTOM_API_KEY).apply()
        GeminiLiveClient.runtimeApiKeyOverride = null
    }

    fun isConfigured(context: Context): Boolean {
        val active = getActiveApiKey(context)
        return active.isNotBlank() && active != "DEFAULT_API_KEY" && active != "MY_GEMINI_API_KEY"
    }

    fun getMaskedKey(context: Context): String {
        val active = getActiveApiKey(context)
        if (active.isBlank()) return "কনফিগার করা নেই (Not configured)"
        if (active.length <= 8) return "****"
        val prefix = active.take(6)
        val suffix = active.takeLast(4)
        return "$prefix...$suffix"
    }
}
