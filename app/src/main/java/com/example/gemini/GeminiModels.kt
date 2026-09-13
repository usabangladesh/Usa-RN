package com.example.gemini

import org.json.JSONArray
import org.json.JSONObject

data class GeminiFunctionCall(
    val name: String,
    val args: JSONObject
)

data class GeminiResponse(
    val text: String?,
    val functionCalls: List<GeminiFunctionCall>,
    val error: String? = null
)
