package com.example.assistant

import android.content.Context
import com.example.apps.AppActionResult
import com.example.apps.UniversalAppControlEngine
import com.example.audio.AudioStreamer
import com.example.gemini.GeminiLiveClient
import com.example.memory.MemoryManager
import com.example.permissions.PermissionManager
import com.example.security.ConfirmationManager
import com.example.security.PendingConfirmation
import com.example.tools.ToolDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Assistant Engine Orchestrator
 * Connects AudioStreamer, LiveSession, ToolDispatcher, and UI state.
 */
class RashedAssistantEngine(
    private val context: Context,
    val toolDispatcher: ToolDispatcher,
    private val confirmationManager: ConfirmationManager,
    private val memoryManager: MemoryManager,
    private val permissionManager: PermissionManager,
    private val universalAppControlEngine: UniversalAppControlEngine
) {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    val audioStreamer = AudioStreamer(
        context = context,
        scope = scope,
        onSpeechRecognized = { text -> handleUserVoiceInput(text) },
        onPartialSpeech = { partial -> _currentTranscript.value = partial },
        onInterruptionDetected = { handleInterruption() }
    )

    val liveSession = LiveSession(
        context = context,
        scope = scope,
        toolDispatcher = toolDispatcher,
        audioStreamer = audioStreamer
    )

    val state: StateFlow<AssistantState> = liveSession.state
    val audioAmplitude: StateFlow<Float> = audioStreamer.amplitude

    private val _currentTranscript = MutableStateFlow("")
    val currentTranscript: StateFlow<String> = _currentTranscript.asStateFlow()

    private val _lastAssistantResponse = MutableStateFlow(
        "Hey handsome! Miss me already? Tap the mic to connect with Mahi."
    )
    val lastAssistantResponse: StateFlow<String> = _lastAssistantResponse.asStateFlow()

    private val _lastActionStatus = MutableStateFlow<String?>(null)
    val lastActionStatus: StateFlow<String?> = _lastActionStatus.asStateFlow()

    val pendingConfirmation: StateFlow<PendingConfirmation?> = confirmationManager.pendingConfirmation
    private var pendingUniversalAction: (suspend () -> AppActionResult)? = null

    init {
        // Collect liveSession updates
        scope.launch {
            liveSession.lastVoiceResponse.collect { resp ->
                if (!resp.isNullOrBlank()) {
                    _lastAssistantResponse.value = resp
                }
            }
        }
        scope.launch {
            liveSession.userTranscript.collect { trans ->
                if (!trans.isNullOrBlank()) {
                    _currentTranscript.value = trans
                }
            }
        }
        scope.launch {
            liveSession.statusText.collect { status ->
                _lastActionStatus.value = status
            }
        }
    }

    fun startVoiceSession() {
        if (!permissionManager.hasRecordAudioPermission()) {
            _lastAssistantResponse.value = "Hey cutie, I need microphone permission to hear your lovely voice!"
            return
        }
        liveSession.startSession()
    }

    fun stopVoiceSession() {
        universalAppControlEngine.stop()
        liveSession.disconnectSession()
    }

    fun toggleVoiceSession() {
        if (state.value == AssistantState.Disconnected || state.value == AssistantState.Error) {
            startVoiceSession()
        } else {
            stopVoiceSession()
        }
    }

    fun emergencyStop() {
        universalAppControlEngine.stop()
        pendingUniversalAction = null
        confirmationManager.clear()
        liveSession.disconnectSession()
        _lastAssistantResponse.value = "All actions stopped, babe. I'm right here whenever you're ready."
        _lastActionStatus.value = "Stopped"
    }

    fun handleInterruption() {
        liveSession.handleInterruption()
    }

    fun handleUserVoiceInput(rawText: String) {
        val text = rawText.trim()
        if (text.isBlank()) return

        _currentTranscript.value = text

        // Check if there is a pending confirmation
        if (confirmationManager.hasPending()) {
            val lower = text.lowercase()
            if (lower.contains("yes") || lower.contains("confirm") || lower.contains("sure") || lower.contains("do it")) {
                confirmPending()
                return
            } else if (lower.contains("no") || lower.contains("cancel") || lower.contains("stop")) {
                cancelPending()
                return
            }
        }

        liveSession.handleUserVoiceInput(text)
    }

    fun confirmPending() {
        scope.launch {
            val res = confirmationManager.confirm()
            val msg = res ?: "Action confirmed, gorgeous!"
            _lastAssistantResponse.value = msg
            audioStreamer.speakText(msg)
        }
    }

    fun cancelPending() {
        scope.launch {
            pendingUniversalAction = null
            val res = confirmationManager.cancel()
            val msg = res ?: "Canceled as requested, babe."
            _lastAssistantResponse.value = msg
            audioStreamer.speakText(msg)
        }
    }

    fun release() {
        scope.cancel()
        audioStreamer.release()
    }
}
