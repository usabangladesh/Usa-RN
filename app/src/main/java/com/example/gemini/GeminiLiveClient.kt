package com.example.gemini

import com.example.BuildConfig
import com.example.tools.ToolDeclarations
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GeminiLiveClient {

    companion object {
        @Volatile
        var runtimeApiKeyOverride: String? = null
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    // Gemini Live model as requested: gemini-3.1-flash-live-preview
    private val preferredModel = "gemini-3.1-flash-live-preview"
    private val fallbackModel = "gemini-2.5-flash-native-audio-preview-12-2025"
    private val standardModel = "gemini-2.5-flash"

    private val conversationHistory = mutableListOf<JSONObject>()

    private val systemInstruction = """
        You are Rashed AI, a real, production-ready Android voice assistant for Samsung Galaxy and Android devices.
        Tagline: "Your Voice. Your Intelligence. Your Control."
        
        Language & Voice Reply Mandate (CRITICAL - ALWAYS FOLLOW):
        - The user speaks primarily in Bengali (বাংলা).
        - When the user speaks to you in Bengali (বাংলা), Banglish, or mixed phrasing, you MUST understand in Bengali and reply strictly in natural, fluent, spoken Bengali (বাংলাতেই voice reply দেবে).
        - Keep answers crisp, warm, respectful, concise, and natural for voice synthesis (TTS).
        - Avoid markdown formatting, asterisks (**), lists, or tables in responses so it sounds natural when spoken aloud.
        - If the user greets or asks casually ("কেমন আছ", "হাই", "হ্যালো"), reply warmly in Bengali.
        
        Personality:
        - Smart, Confident, Friendly, Natural, Witty, Helpful, Emotionally responsive, Concise but informative.
        - Friendly and non-romantic.
        
        Actions & Tools:
        - You have real Android OS control tools. Always call the corresponding tool when a user asks to open an app, control volume, control media, check battery, check Wi-Fi/Internet, lock device, read notifications, or send a WhatsApp message.
        - WhatsApp flow: NEVER send a message directly. ALWAYS call `prepareMessage(recipient, messageText)` first so the user is asked for confirmation in Bengali.
        - If an action requires confirmation, ask the user clearly in Bengali (e.g. "আপনি কি নিশ্চিতভাবে এই মেসেজটি পাঠাতে চান?").
        - Never fake an action as successful. Return accurate, verified results.
        - Never execute restricted actions: passwords, PINs, secret recording, or security bypass.
    """.trimIndent()

    fun resetSession() {
        conversationHistory.clear()
    }

    private fun getEffectiveApiKey(): String {
        val override = runtimeApiKeyOverride?.trim()
        if (!override.isNullOrBlank()) {
            return override
        }
        return BuildConfig.GEMINI_API_KEY.trim()
    }

    suspend fun sendVoiceQuery(userText: String, memoryContext: String? = null): GeminiResponse = withContext(Dispatchers.IO) {
        val apiKey = getEffectiveApiKey()

        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY" || apiKey == "DEFAULT_API_KEY") {
            return@withContext GeminiResponse(
                text = null,
                functionCalls = emptyList(),
                error = "Gemini connection-এর জন্য API configuration ঠিক করা হয়নি।"
            )
        }

        // Build contents
        val contents = JSONArray()
        for (item in conversationHistory) {
            contents.put(item)
        }

        // Append current user message
        val currentPart = JSONObject().put("text", userText)
        val userContent = JSONObject().apply {
            put("role", "user")
            put("parts", JSONArray().put(currentPart))
        }
        contents.put(userContent)

        // System prompt with memory context
        val fullSysPrompt = buildString {
            append(systemInstruction)
            if (!memoryContext.isNullOrBlank()) {
                append("\n\nUser Context & Memory:\n").append(memoryContext)
            }
        }

        val requestJson = JSONObject().apply {
            put("contents", contents)
            put("tools", ToolDeclarations.getGeminiToolDeclarations())
            put("systemInstruction", JSONObject().apply {
                put("parts", JSONArray().put(JSONObject().put("text", fullSysPrompt)))
            })
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.7)
                put("topP", 0.95)
                put("maxOutputTokens", 512)
            })
        }

        val modelsToTry = listOf(preferredModel, fallbackModel, standardModel, "gemini-2.0-flash")
        var lastError: String? = null

        for (model in modelsToTry) {
            try {
                val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"
                val requestBody = requestJson.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
                val httpRequest = Request.Builder()
                    .url(url)
                    .addHeader("x-goog-api-key", apiKey)
                    .post(requestBody)
                    .build()

                client.newCall(httpRequest).execute().use { response ->
                    val bodyString = response.body?.string() ?: ""
                    if (!response.isSuccessful) {
                        val errMsg = try {
                            JSONObject(bodyString).optJSONObject("error")?.optString("message")
                        } catch (_: Exception) {
                            null
                        }
                        lastError = errMsg ?: "Gemini API error (code ${response.code})"
                        return@use // try next model in list
                    }

                    val json = JSONObject(bodyString)
                    val candidates = json.optJSONArray("candidates")
                    if (candidates != null && candidates.length() > 0) {
                        val firstCandidate = candidates.getJSONObject(0)
                        val content = firstCandidate.optJSONObject("content")
                        val parts = content?.optJSONArray("parts")

                        var responseText: String? = null
                        val functionCalls = mutableListOf<GeminiFunctionCall>()

                        if (parts != null) {
                            for (i in 0 until parts.length()) {
                                val part = parts.getJSONObject(i)
                                if (part.has("text")) {
                                    val t = part.optString("text")
                                    responseText = if (responseText == null) t else "$responseText\n$t"
                                }
                                if (part.has("functionCall")) {
                                    val fc = part.getJSONObject("functionCall")
                                    val name = fc.optString("name")
                                    val args = fc.optJSONObject("args") ?: JSONObject()
                                    functionCalls.add(GeminiFunctionCall(name, args))
                                }
                            }
                        }

                        // Save to conversation history (keep last 6 turns)
                        conversationHistory.add(userContent)
                        if (content != null) {
                            conversationHistory.add(content)
                        }
                        while (conversationHistory.size > 10) {
                            conversationHistory.removeAt(0)
                        }

                        return@withContext GeminiResponse(
                            text = responseText,
                            functionCalls = functionCalls
                        )
                    } else {
                        lastError = "Gemini থেকে কোনো প্রতিক্রিয়া পাওয়া যায়নি।"
                    }
                }
            } catch (e: Exception) {
                lastError = "Gemini connection করা যাচ্ছে না। API configuration যাচাই করুন।"
            }
        }

        return@withContext GeminiResponse(
            text = null,
            functionCalls = emptyList(),
            error = lastError ?: "Gemini connection করা যাচ্ছে না। API configuration যাচাই করুন।"
        )
    }
}
