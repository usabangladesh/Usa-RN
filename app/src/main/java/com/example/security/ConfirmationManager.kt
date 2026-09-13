package com.example.security

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PendingConfirmation(
    val actionId: String,
    val title: String,
    val promptText: String,
    val recipient: String? = null,
    val content: String? = null,
    val onConfirm: suspend () -> String,
    val onCancel: suspend () -> String
)

class ConfirmationManager {
    private val _pendingConfirmation = MutableStateFlow<PendingConfirmation?>(null)
    val pendingConfirmation: StateFlow<PendingConfirmation?> = _pendingConfirmation.asStateFlow()

    fun requestConfirmation(confirmation: PendingConfirmation) {
        _pendingConfirmation.value = confirmation
    }

    suspend fun confirm(): String? {
        val pending = _pendingConfirmation.value ?: return null
        _pendingConfirmation.value = null
        return pending.onConfirm.invoke()
    }

    suspend fun cancel(): String? {
        val pending = _pendingConfirmation.value ?: return null
        _pendingConfirmation.value = null
        return pending.onCancel.invoke()
    }

    fun clear() {
        _pendingConfirmation.value = null
    }

    fun hasPending(): Boolean = _pendingConfirmation.value != null
}
