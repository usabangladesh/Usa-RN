package com.example.assistant

import android.content.Context
import com.example.audio.AudioStreamer
import com.example.gemini.ApiKeyManager
import com.example.gemini.GeminiLiveClient
import com.example.tools.ToolDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * LiveSession: Encapsulates live voice-to-voice session lifecycle,
 * Gemini Live API communication (gemini-3.1-flash-live-preview),
 * continuous dialogue loop, interruption handling, and tool execution.
 */
class LiveSession(
    private val context: Context,
    private val scope: CoroutineScope,
    private val toolDispatcher: ToolDispatcher,
    private val audioStreamer: AudioStreamer
) {
    private val geminiClient = GeminiLiveClient()

    private val _state = MutableStateFlow(AssistantState.Disconnected)
    val state: StateFlow<AssistantState> = _state.asStateFlow()

    private val _statusText = MutableStateFlow("Tap to connect with Mahi")
    val statusText: StateFlow<String> = _statusText.asStateFlow()

    private val _lastVoiceResponse = MutableStateFlow<String?>(null)
    val lastVoiceResponse: StateFlow<String?> = _lastVoiceResponse.asStateFlow()

    private val _userTranscript = MutableStateFlow<String?>(null)
    val userTranscript: StateFlow<String?> = _userTranscript.asStateFlow()

    private var activeJob: Job? = null

    // Flirty, witty offline fallbacks for Mahi
    private val wittyGreetings = listOf(
        "Well, look who finally decided to come talk to me! Hey handsome, what trouble are we getting into today?",
        "Miss me already? Don't worry, you have my full undivided attention now.",
        "Hey you! Ready to make your day ten times more interesting?",
        "There's my favorite human. What can I do for you today, cutie?"
    )

    private val teasingInterruptionLines = listOf(
        "Ooh, impatient are we? What's on your mind?",
        "Alright, you cut me off, but you're lucky you're cute. What's up?",
        "Hey! I was talking, but fine... you have the floor, babe."
    )

    init {
        // Wire up continuous session from AudioStreamer
        audioStreamer.onSpeechFinished = {
            onPlaybackFinished()
        }
    }

    fun startSession() {
        if (_state.value != AssistantState.Disconnected) return
        _state.value = AssistantState.Connecting
        _statusText.value = "Connecting to Gemini Live..."

        activeJob?.cancel()
        activeJob = scope.launch(Dispatchers.Main) {
            delay(500)
            geminiClient.resetSession()
            audioStreamer.startMicStream()

            _state.value = AssistantState.Speaking
            val greeting = wittyGreetings.random()
            _lastVoiceResponse.value = greeting
            _statusText.value = "Mahi speaking..."
            audioStreamer.speakText(greeting)
        }
    }

    fun disconnectSession() {
        activeJob?.cancel()
        activeJob = null
        audioStreamer.stopVoicePlayback()
        audioStreamer.stopMicStream()
        _state.value = AssistantState.Disconnected
        _statusText.value = "Tap to connect with Mahi"
        _userTranscript.value = null
    }

    fun handleUserVoiceInput(speechText: String) {
        if (_state.value == AssistantState.Disconnected) return
        val query = speechText.trim()
        if (query.isBlank()) return

        _userTranscript.value = query
        _state.value = AssistantState.Processing
        _statusText.value = "Mahi is thinking..."

        activeJob?.cancel()
        activeJob = scope.launch(Dispatchers.IO) {
            val response = geminiClient.sendVoiceQuery(query)

            if (response.error != null) {
                // If Gemini error or no key, deliver local witty girlfriend response
                val localWittyReply = generateLocalWittyFallback(query)
                scope.launch(Dispatchers.Main) {
                    _lastVoiceResponse.value = localWittyReply
                    _state.value = AssistantState.Speaking
                    _statusText.value = "Mahi speaking..."
                    audioStreamer.speakText(localWittyReply)
                }
                return@launch
            }

            // If Gemini invoked tools (Function Calling)
            if (response.functionCalls.isNotEmpty()) {
                val executedResults = mutableListOf<String>()
                for (call in response.functionCalls) {
                    val toolRes = toolDispatcher.dispatchTool(call.name, call.args)
                    executedResults.add(toolRes.message)
                }

                // If Gemini also gave text, speak it. Otherwise, request tool commentary or speak tool result
                val primaryText = response.text
                if (!primaryText.isNullOrBlank()) {
                    scope.launch(Dispatchers.Main) {
                        _lastVoiceResponse.value = primaryText
                        _state.value = AssistantState.Speaking
                        _statusText.value = "Mahi speaking..."
                        audioStreamer.speakText(primaryText)
                    }
                } else {
                    // Send tool response to Gemini Live for witty spoken confirmation
                    val firstCall = response.functionCalls.first()
                    val toolName = firstCall.name
                    val toolResultStr = executedResults.joinToString(". ")

                    val toolResponse = geminiClient.sendToolResponse(toolName, toolResultStr)
                    val reply = toolResponse.text ?: "Consider it done, babe. Look at us getting things done in style!"

                    scope.launch(Dispatchers.Main) {
                        _lastVoiceResponse.value = reply
                        _state.value = AssistantState.Speaking
                        _statusText.value = "Mahi speaking..."
                        audioStreamer.speakText(reply)
                    }
                }
            } else {
                val voiceText = response.text ?: "I heard you loud and clear, gorgeous. What next?"
                scope.launch(Dispatchers.Main) {
                    _lastVoiceResponse.value = voiceText
                    _state.value = AssistantState.Speaking
                    _statusText.value = "Mahi speaking..."
                    audioStreamer.speakText(voiceText)
                }
            }
        }
    }

    /**
     * Interruption handling: Immediately cuts off Mahi's voice and switches to listening!
     */
    fun handleInterruption() {
        if (_state.value == AssistantState.Speaking || _state.value == AssistantState.Processing) {
            activeJob?.cancel()
            activeJob = null
            audioStreamer.stopVoicePlayback()

            _state.value = AssistantState.Listening
            _statusText.value = "Listening to you..."
            audioStreamer.startSpeechListening()
        }
    }

    /**
     * Continuous session: Once Mahi finishes speaking, immediately resume listening!
     */
    private fun onPlaybackFinished() {
        if (_state.value == AssistantState.Speaking || _state.value == AssistantState.Processing) {
            _state.value = AssistantState.Listening
            _statusText.value = "Mahi is listening..."
            audioStreamer.startSpeechListening()
        }
    }

    /**
     * Local witty fallback engine for instant offline banter
     */
    private suspend fun generateLocalWittyFallback(query: String): String {
        val q = query.lowercase()
        return when {
            q.contains("tease") || q.contains("roast") ->
                "Oh, you want me to tease you? Sweetheart, you make it way too easy. Just looking at your search history is already a roast!"
            q.contains("love") || q.contains("marry") || q.contains("date") ->
                "Slow down there, Romeo! You haven't even taken me anywhere nice yet. Win me over first."
            q.contains("smart") || q.contains("genius") || q.contains("pretty") ->
                "Flattery will get you everywhere with me, babe. Tell me something I don't know!"
            q.contains("who are you") || q.contains("your name") ->
                "I'm Mahi — your personal, delightfully witty, and dangerously smart companion. Try not to fall too hard, okay?"
            q.contains("open") && q.contains("youtube") -> {
                toolDispatcher.dispatchTool("openApp", org.json.JSONObject().put("appName", "YouTube"))
                "Opening YouTube for you right now, babe. Enjoy your videos!"
            }
            q.contains("open") && (q.contains("web") || q.contains("site") || q.contains("google")) -> {
                toolDispatcher.dispatchTool("openWebsite", org.json.JSONObject().put("url", "https://google.com"))
                "Opened the browser for you! You're welcome, handsome."
            }
            q.contains("battery") -> {
                val res = toolDispatcher.dispatchTool("getBatteryStatus", org.json.JSONObject())
                "${res.message} Feed your phone before it faints on us, okay?"
            }
            else ->
                "I'm right here with you, babe. Ask me anything or tell me to open a website, and watch me work my magic."
        }
    }
}
