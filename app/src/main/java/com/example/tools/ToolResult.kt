package com.example.tools

data class ToolResult(
    val success: Boolean,
    val message: String,
    val toolName: String,
    val verified: Boolean = true,
    val error: String? = null,
    val requiresConfirmation: Boolean = false,
    val pendingActionPrompt: String? = null,
    val payload: Map<String, Any?> = emptyMap()
)
