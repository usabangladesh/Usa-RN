package com.example.permissions

enum class PermissionLevel {
    SAFE,
    CONFIRMATION_REQUIRED,
    RESTRICTED
}

data class ToolSecurityPolicy(
    val toolName: String,
    val level: PermissionLevel,
    val description: String
)
