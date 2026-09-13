package com.example.audio

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.abs
import kotlin.math.sqrt

class AudioEngine(
    private val context: Context,
    private val scope: CoroutineScope,
    private val onUserSpoke: (String) -> Unit,
    private val onUserInterrupted: () -> Unit
) {

    private val _audioAmplitude = MutableStateFlow(0f)
    val audioAmplitude: StateFlow<Float> = _audioAmplitude.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private var audioRecord: AudioRecord? = null
    private var recordJob: Job? = null
    private var tts: TextToSpeech? = null
    private var isTtsReady = false

    private var speechRecognizer: SpeechRecognizer? = null
    private var isListeningForSpeech = false

    init {
        initTts()
    }

    private fun initTts() {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isTtsReady = true
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        _isSpeaking.value = true
                    }

                    override fun onDone(utteranceId: String?) {
                        _isSpeaking.value = false
                    }

                    override fun onError(utteranceId: String?) {
                        _isSpeaking.value = false
                    }
                })
            }
        }
    }

    fun speak(text: String, onComplete: (() -> Unit)? = null) {
        if (!isTtsReady || text.isBlank()) {
            onComplete?.invoke()
            return
        }

        stopSpeaking()

        // Select optimal language voice
        val targetLocale = when {
            text.any { it in '\u0980'..'\u09FF' } -> Locale.forLanguageTag("bn-BD")
            text.any { it in '\u0900'..'\u097F' } -> Locale.forLanguageTag("hi-IN")
            else -> Locale.US
        }

        try {
            val result = tts?.setLanguage(targetLocale)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                // Fallback to English
                tts?.language = Locale.US
            }
            tts?.setPitch(1.05f)
            tts?.setSpeechRate(1.0f)

            val utteranceId = "Rashed_${System.currentTimeMillis()}"
            val params = Bundle().apply {
                putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
            }

            _isSpeaking.value = true
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
        } catch (_: Exception) {
            _isSpeaking.value = false
            onComplete?.invoke()
        }
    }

    fun stopSpeaking() {
        try {
            if (tts?.isSpeaking == true) {
                tts?.stop()
            }
        } catch (_: Exception) {}
        _isSpeaking.value = false
    }

    @SuppressLint("MissingPermission")
    fun startAudioStream() {
        if (_isRecording.value) return

        val sampleRate = 16000
        val channelConfig = AudioFormat.CHANNEL_IN_MONO
        val audioFormat = AudioFormat.ENCODING_PCM_16BIT
        val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat).coerceAtLeast(2048)

        try {
            val record = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize
            )
            if (record.state != AudioRecord.STATE_INITIALIZED) {
                return
            }

            audioRecord = record
            record.startRecording()
            _isRecording.value = true

            recordJob = scope.launch(Dispatchers.Default) {
                val buffer = ShortArray(bufferSize / 2)
                var speechFramesCount = 0

                while (isActive && _isRecording.value) {
                    val read = record.read(buffer, 0, buffer.size)
                    if (read > 0) {
                        var sum = 0.0
                        for (i in 0 until read) {
                            sum += buffer[i] * buffer[i]
                        }
                        val rms = sqrt(sum / read)
                        val normalized = (rms / 32768.0).toFloat().coerceIn(0f, 1f)
                        _audioAmplitude.value = normalized

                        // Voice Activity Detection (VAD) for interruption handling
                        if (normalized > 0.18f) {
                            speechFramesCount++
                            if (speechFramesCount > 3 && _isSpeaking.value) {
                                // User interrupted Rashed speaking
                                scope.launch(Dispatchers.Main) {
                                    stopSpeaking()
                                    onUserInterrupted.invoke()
                                }
                                speechFramesCount = 0
                            }
                        } else {
                            if (speechFramesCount > 0) speechFramesCount--
                        }
                    }
                }
            }
        } catch (_: Exception) {
            _isRecording.value = false
        }
    }

    fun stopAudioStream() {
        _isRecording.value = false
        recordJob?.cancel()
        recordJob = null
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (_: Exception) {}
        audioRecord = null
        _audioAmplitude.value = 0f
    }

    fun startListeningForVoiceCommand() {
        if (isListeningForSpeech) return

        scope.launch(Dispatchers.Main) {
            try {
                if (speechRecognizer == null) {
                    speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
                }

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "bn-BD")
                    putExtra(RecognizerIntent.EXTRA_SUPPORTED_LANGUAGES, arrayListOf("bn-BD", "en-US", "hi-IN"))
                }

                speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        isListeningForSpeech = true
                    }

                    override fun onBeginningOfSpeech() {
                        if (_isSpeaking.value) {
                            stopSpeaking()
                            onUserInterrupted.invoke()
                        }
                    }

                    override fun onRmsChanged(rmsdB: Float) {
                        val level = (rmsdB / 12f).coerceIn(0f, 1f)
                        if (!_isRecording.value) {
                            _audioAmplitude.value = level
                        }
                    }

                    override fun onBufferReceived(buffer: ByteArray?) {}

                    override fun onEndOfSpeech() {
                        isListeningForSpeech = false
                    }

                    override fun onError(error: Int) {
                        isListeningForSpeech = false
                    }

                    override fun onResults(results: Bundle?) {
                        isListeningForSpeech = false
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val spoken = matches?.firstOrNull()
                        if (!spoken.isNullOrBlank()) {
                            onUserSpoke.invoke(spoken)
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val partial = matches?.firstOrNull()
                        if (!partial.isNullOrBlank() && _isSpeaking.value) {
                            stopSpeaking()
                            onUserInterrupted.invoke()
                        }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })

                speechRecognizer?.startListening(intent)
            } catch (_: Exception) {
                isListeningForSpeech = false
            }
        }
    }

    fun stopListeningForVoiceCommand() {
        scope.launch(Dispatchers.Main) {
            try {
                speechRecognizer?.stopListening()
            } catch (_: Exception) {}
            isListeningForSpeech = false
        }
    }

    fun emergencyStop() {
        stopSpeaking()
        stopListeningForVoiceCommand()
        stopAudioStream()
    }

    fun release() {
        emergencyStop()
        try {
            speechRecognizer?.destroy()
            tts?.shutdown()
        } catch (_: Exception) {}
    }
}
