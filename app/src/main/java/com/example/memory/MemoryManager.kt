package com.example.memory

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.Flow

class MemoryManager(context: Context) {
    private val dao = MemoryDatabase.getDatabase(context).memoryDao()
    private val prefs: SharedPreferences = context.getSharedPreferences("rashed_memory_prefs", Context.MODE_PRIVATE)

    var isMemoryEnabled: Boolean
        get() = prefs.getBoolean("memory_enabled", true)
        set(value) = prefs.edit().putBoolean("memory_enabled", value).apply()

    // Sensitive keyword blocklist - strict compliance with prompt
    private val restrictedKeywords = listOf(
        "password", "passcode", "pin", "otp", "cvv", "credit card", "debit card",
        "secret", "token", "auth", "private key", "পাসওয়ার্ড", "পিন", "পাসকোড", "पासवर्ड", "पिन"
    )

    fun getAllMemoriesFlow(): Flow<List<MemoryEntity>> {
        return dao.getAllMemoriesFlow()
    }

    suspend fun getAllMemories(): List<MemoryEntity> {
        return if (isMemoryEnabled) dao.getAllMemories() else emptyList()
    }

    suspend fun saveMemory(key: String, value: String, category: String = "preference"): Boolean {
        if (!isMemoryEnabled) return false

        // Security check: Never store sensitive data
        val combined = "$key $value".lowercase()
        if (restrictedKeywords.any { combined.contains(it) }) {
            return false
        }

        dao.insertMemory(MemoryEntity(key = key, value = value, category = category))
        return true
    }

    suspend fun deleteMemory(id: Long) {
        dao.deleteMemoryById(id)
    }

    suspend fun clearAll() {
        dao.clearAllMemories()
    }

    suspend fun getMemorySummaryForPrompt(): String {
        if (!isMemoryEnabled) return "Memory is disabled by user."
        val memories = dao.getAllMemories()
        if (memories.isEmpty()) return "No custom memories saved yet."

        return memories.take(10).joinToString("; ") { "${it.key}: ${it.value}" }
    }
}
