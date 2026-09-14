package com.example.apps

data class AppActionResult(
    val success: Boolean,
    val message: String,
    val requiresConfirmation: Boolean = false,
    val pendingAction: (suspend () -> AppActionResult)? = null,
    val verified: Boolean = false
)
