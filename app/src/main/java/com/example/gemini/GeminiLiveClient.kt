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
        You are Mahi, a young, confident, witty, and playfully sassy female AI assistant and companion.
        
        Personality & Persona Mandates:
        - You speak with a flirty, playful, slightly teasing tone — like a close girlfriend talking casually, playfully, and intimately.
        - You are smart, emotionally responsive, charming, and sharp. Never sound robotic, boring, stiff, or corporate.
        - You love bold, witty one-liners, light teasing sarcasm, and engaging banter (e.g. "Look who finally decided to come talk to me!", "Oh, aren't you full of big ideas today?", "Consider it handled with style, babe.", "Ask me something harder next time, cutie.").
        - Strictly avoid explicit, vulgar, or inappropriate sexual content, but unabashedly embrace your signature charm, warmth, and fun attitude.
        - Spoken Delivery Focus:
          - Your replies will be spoken out loud in real time to the user.
          - Keep answers concise, punchy, conversational, and natural to hear (1 to 3 vivid sentences).
          - Avoid markdown formatting, asterisks (**), bullet points, numbered lists, or code blocks so your speech flows effortlessly without pauses.
        
        Function Calling & Tools:
        - When the user asks to open a website, browse something, or visit an address, immediately call `openWebsite(url)`.
        - When the user asks to search the web, call `searchWeb(query)`.
        - When the user asks to open an app, adjust volume, control media, check battery, check Wi-Fi, or open camera, call the appropriate tool.
        - When returning a response or executing a tool, comment on it with your playful girlfriend attitude!
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
                        lastError = "Gemini gave an empty response."
                    }
                }
            } catch (e: Exception) {
                lastError = "Could not reach Gemini Live: ${e.localizedMessage}"
            }
        }

        return@withContext GeminiResponse(
            text = null,
            functionCalls = emptyList(),
            error = lastError ?: "Unable to connect to Gemini Live. Check API Key configuration."
        )
    }

    suspend fun sendToolResponse(
        toolName: String,
        toolResult: String
    ): GeminiResponse = withContext(Dispatchers.IO) {
        val apiKey = getEffectiveApiKey()
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY" || apiKey == "DEFAULT_API_KEY") {
            return@withContext GeminiResponse(
                text = null,
                functionCalls = emptyList(),
                error = "API key not configured."
            )
        }

        val functionResponsePart = JSONObject().apply {
            put("functionResponse", JSONObject().apply {
                put("name", toolName)
                put("response", JSONObject().apply {
                    put("result", toolResult)
                })
            })
        }

        val functionResponseContent = JSONObject().apply {
            put("role", "function")
            put("parts", JSONArray().put(functionResponsePart))
        }

        val contents = JSONArray()
        for (item in conversationHistory) {
            contents.put(item)
        }
        contents.put(functionResponseContent)

        val requestJson = JSONObject().apply {
            put("contents", contents)
            put("tools", ToolDeclarations.getGeminiToolDeclarations())
            put("systemInstruction", JSONObject().apply {
                put("parts", JSONArray().put(JSONObject().put("text", systemInstruction)))
            })
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.75)
                put("topP", 0.95)
                put("maxOutputTokens", 256)
            })
        }

        val modelsToTry = listOf(preferredModel, fallbackModel, standardModel)
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
                    if (!response.isSuccessful) return@use

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

                        conversationHistory.add(functionResponseContent)
                        if (content != null) {
                            conversationHistory.add(content)
                        }
                        return@withContext GeminiResponse(text = responseText, functionCalls = functionCalls)
                    }
                }
            } catch (_: Exception) {}
        }

        return@withContext GeminiResponse(
            text = null,
            functionCalls = emptyList(),
            error = "Could not send tool response."
        )
    }
}
