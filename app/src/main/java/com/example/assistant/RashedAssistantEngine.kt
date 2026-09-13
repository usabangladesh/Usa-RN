package com.example.assistant

import android.content.Context
import com.example.audio.AudioEngine
import com.example.gemini.GeminiLiveClient
import com.example.intent.IntentRouter
import com.example.intent.ParsedIntent
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
import org.json.JSONObject

class RashedAssistantEngine(
    private val context: Context,
    private val toolDispatcher: ToolDispatcher,
    private val confirmationManager: ConfirmationManager,
    private val memoryManager: MemoryManager,
    private val permissionManager: PermissionManager
) {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val geminiClient = GeminiLiveClient()

    private val _state = MutableStateFlow(AssistantState.Disconnected)
    val state: StateFlow<AssistantState> = _state.asStateFlow()

    private val _currentTranscript = MutableStateFlow("")
    val currentTranscript: StateFlow<String> = _currentTranscript.asStateFlow()

    private val _lastAssistantResponse = MutableStateFlow("Rashed AI প্রস্তুত। কথা বলতে মাইক্রোফোনে ট্যাপ করুন।")
    val lastAssistantResponse: StateFlow<String> = _lastAssistantResponse.asStateFlow()

    private val _lastActionStatus = MutableStateFlow<String?>(null)
    val lastActionStatus: StateFlow<String?> = _lastActionStatus.asStateFlow()

    val pendingConfirmation: StateFlow<PendingConfirmation?> = confirmationManager.pendingConfirmation

    val audioEngine = AudioEngine(
        context = context,
        scope = scope,
        onUserSpoke = { speech -> handleUserVoiceInput(speech) },
        onUserInterrupted = { handleInterruption() }
    )

    val audioAmplitude: StateFlow<Float> = audioEngine.audioAmplitude

    fun startVoiceSession() {
        if (!permissionManager.hasRecordAudioPermission()) {
            _state.value = AssistantState.Error
            _lastAssistantResponse.value = "মাইক্রোফোন পারমিশন প্রয়োজন। অনুগ্রহ করে অনুমতি দিন।"
            return
        }

        _state.value = AssistantState.Connecting
        audioEngine.startAudioStream()
        startListening()
    }

    private fun startListening() {
        _state.value = AssistantState.Listening
        audioEngine.startListeningForVoiceCommand()
    }

    fun stopVoiceSession() {
        audioEngine.stopListeningForVoiceCommand()
        audioEngine.stopAudioStream()
        audioEngine.stopSpeaking()
        _state.value = AssistantState.Disconnected
    }

    fun toggleVoiceSession() {
        if (_state.value == AssistantState.Disconnected || _state.value == AssistantState.Error) {
            startVoiceSession()
        } else {
            stopVoiceSession()
        }
    }

    fun emergencyStop() {
        audioEngine.emergencyStop()
        confirmationManager.clear()
        _state.value = AssistantState.Disconnected
        _lastAssistantResponse.value = "জরুরি স্টপ কার্যকর করা হয়েছে।"
        _lastActionStatus.value = "Emergency Stop"
    }

    private fun handleInterruption() {
        audioEngine.stopSpeaking()
        startListening()
    }

    fun handleUserVoiceInput(rawText: String) {
        val text = rawText.trim()
        if (text.isBlank()) return

        _currentTranscript.value = text
        _state.value = AssistantState.Processing

        scope.launch {
            // Check if there is an active pending confirmation
            if (confirmationManager.hasPending()) {
                val parsed = IntentRouter.parseUserCommand(text)
                when (parsed) {
                    is ParsedIntent.UserConfirmed -> {
                        val result = confirmationManager.confirm()
                        speakAndShowResponse(result ?: "মেসেজ পাঠানো হয়েছে।", "Action Confirmed")
                        return@launch
                    }
                    is ParsedIntent.UserCancelled -> {
                        val result = confirmationManager.cancel()
                        speakAndShowResponse(result ?: "বাতিল করা হয়েছে।", "Action Cancelled")
                        return@launch
                    }
                    is ParsedIntent.EmergencyStop -> {
                        emergencyStop()
                        return@launch
                    }
                    else -> {
                        // User ignored confirmation and issued another command; cancel previous pending
                        confirmationManager.cancel()
                    }
                }
            }

            // Route user command
            val intent = IntentRouter.parseUserCommand(text)
            when (intent) {
                is ParsedIntent.EmergencyStop -> {
                    emergencyStop()
                }

                is ParsedIntent.UserConfirmed -> {
                    speakAndShowResponse("কোনো নিশ্চিতকরণ বাকি নেই। আপনি কী করতে চান বলুন।", null)
                }

                is ParsedIntent.UserCancelled -> {
                    speakAndShowResponse("ঠিক আছে, বাতিল করা হলো।", null)
                }

                is ParsedIntent.DirectTool -> {
                    executeToolDirectly(intent.toolName, intent.args)
                }

                is ParsedIntent.GeneralGemini -> {
                    executeWithGemini(text)
                }
            }
        }
    }

    private suspend fun executeToolDirectly(toolName: String, args: JSONObject) {
        val result = toolDispatcher.dispatchTool(toolName, args)
        if (result.requiresConfirmation) {
            speakAndShowResponse(result.message, "Confirmation Needed")
        } else {
            val statusTag = if (result.success) "✓ Success: $toolName" else "⚠ Failed: $toolName"
            speakAndShowResponse(result.message, statusTag)
        }
    }

    private suspend fun executeWithGemini(userText: String) {
        val memoryContext = memoryManager.getMemorySummaryForPrompt()
        val response = geminiClient.sendVoiceQuery(userText, memoryContext)

        if (response.functionCalls.isNotEmpty()) {
            // Execute function calls
            val executionResults = mutableListOf<String>()
            var requiresConfirm = false

            for (fc in response.functionCalls) {
                val tr = toolDispatcher.dispatchTool(fc.name, fc.args)
                if (tr.requiresConfirmation) {
                    requiresConfirm = true
                    speakAndShowResponse(tr.message, "Confirmation Needed")
                    return
                } else {
                    executionResults.add(tr.message)
                }
            }

            val combinedMsg = executionResults.joinToString("\n")
            speakAndShowResponse(combinedMsg, "Tools Executed")
        } else if (!response.text.isNullOrBlank()) {
            speakAndShowResponse(response.text, null)
        } else {
            // Fallback if network or Gemini key unavailable
            val fallbackMsg = response.error ?: "দুঃখিত, সংযোগে সমস্যা হয়েছে। আপনি সরাসরি কোনো কমান্ড বলতে পারেন।"
            speakAndShowResponse(fallbackMsg, "Gemini Unavailable")
        }
    }

    private fun speakAndShowResponse(responseMessage: String, status: String?) {
        _lastAssistantResponse.value = responseMessage
        _lastActionStatus.value = status
        _state.value = AssistantState.Speaking

        audioEngine.speak(responseMessage) {
            // When speaking completes, resume listening seamlessly
            if (_state.value == AssistantState.Speaking) {
                startListening()
            }
        }
    }

    fun confirmPending() {
        scope.launch {
            val res = confirmationManager.confirm()
            speakAndShowResponse(res ?: "সম্পন্ন হয়েছে।", "Confirmed")
        }
    }

    fun cancelPending() {
        scope.launch {
            val res = confirmationManager.cancel()
            speakAndShowResponse(res ?: "বাতিল করা হয়েছে।", "Cancelled")
        }
    }

    fun release() {
        scope.cancel()
        audioEngine.release()
    }
}
