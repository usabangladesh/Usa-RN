package com.example.audio

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.sqrt

/**
 * AudioStreamer: Dedicated clean architecture layer for audio capture,
 * 16kHz PCM mic streaming, 24kHz audio playback, VAD, and speech synthesis.
 */
class AudioStreamer(
    private val context: Context,
    private val scope: CoroutineScope,
    private val onSpeechRecognized: (String) -> Unit,
    private val onPartialSpeech: (String) -> Unit = {},
    private val onInterruptionDetected: () -> Unit = {}
) {
    companion object {
        const val SAMPLE_RATE_IN_16K = 16000
        const val SAMPLE_RATE_OUT_24K = 24000
        private const val VAD_INTERRUPTION_THRESHOLD = 0.22f
    }

    private val _amplitude = MutableStateFlow(0f)
    val amplitude: StateFlow<Float> = _amplitude.asStateFlow()

    private val _isStreaming = MutableStateFlow(false)
    val isStreaming: StateFlow<Boolean> = _isStreaming.asStateFlow()

    private val _isPlayingVoice = MutableStateFlow(false)
    val isPlayingVoice: StateFlow<Boolean> = _isPlayingVoice.asStateFlow()

    // 16kHz PCM AudioRecord
    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null

    // 24kHz AudioTrack
    private var audioTrack: AudioTrack? = null

    // Speech Recognizer
    private var speechRecognizer: SpeechRecognizer? = null
    private var recognizerIntent: Intent? = null
    private var isRecognizerListening = false

    // Female Text-To-Speech
    private var tts: TextToSpeech? = null
    private var isTtsReady = false
    private var speechSimulationJob: Job? = null

    // Callback when speech playback finishes
    var onSpeechFinished: (() -> Unit)? = null

    init {
        initTts()
        initSpeechRecognizer()
    }

    private fun initTts() {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.let { engine ->
                    // Set female voice characteristics
                    engine.language = Locale.US
                    engine.setPitch(1.16f) // Youthful, bright, vibrant pitch
                    engine.setSpeechRate(1.05f) // Conversational, confident speed

                    // Try to select the highest quality female voice available
                    try {
                        val voices = engine.voices
                        if (!voices.isNullOrEmpty()) {
                            val femaleVoice = voices.firstOrNull { voice ->
                                val name = voice.name.lowercase(Locale.ROOT)
                                !voice.isNetworkConnectionRequired &&
                                        (name.contains("female") || name.contains("en-us-x-sfg") || name.contains("en-us-x-tpd"))
                            } ?: voices.firstOrNull { voice ->
                                voice.name.lowercase(Locale.ROOT).contains("female")
                            } ?: voices.firstOrNull { voice ->
                                voice.locale.language == "en"
                            }

                            if (femaleVoice != null) {
                                engine.voice = femaleVoice
                            }
                        }
                    } catch (_: Exception) {}

                    engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) {
                            _isPlayingVoice.value = true
                            startSpeechWaveSimulation()
                        }

                        override fun onDone(utteranceId: String?) {
                            _isPlayingVoice.value = false
                            stopSpeechWaveSimulation()
                            scope.launch(Dispatchers.Main) {
                                onSpeechFinished?.invoke()
                            }
                        }

                        override fun onError(utteranceId: String?) {
                            _isPlayingVoice.value = false
                            stopSpeechWaveSimulation()
                            scope.launch(Dispatchers.Main) {
                                onSpeechFinished?.invoke()
                            }
                        }
                    })

                    isTtsReady = true
                }
            }
        }
    }

    private fun initSpeechRecognizer() {
        scope.launch(Dispatchers.Main) {
            if (SpeechRecognizer.isRecognitionAvailable(context)) {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                    setRecognitionListener(object : RecognitionListener {
                        override fun onReadyForSpeech(params: Bundle?) {
                            isRecognizerListening = true
                        }

                        override fun onBeginningOfSpeech() {
                            // User started speaking! Check for interruption if assistant was talking
                            if (_isPlayingVoice.value) {
                                stopVoicePlayback()
                                onInterruptionDetected()
                            }
                        }

                        override fun onRmsChanged(rmsdB: Float) {
                            if (!_isPlayingVoice.value) {
                                val normalized = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
                                _amplitude.value = normalized
                            }
                        }

                        override fun onBufferReceived(buffer: ByteArray?) {}

                        override fun onEndOfSpeech() {
                            isRecognizerListening = false
                        }

                        override fun onError(error: Int) {
                            isRecognizerListening = false
                        }

                        override fun onResults(results: Bundle?) {
                            isRecognizerListening = false
                            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            val text = matches?.firstOrNull()?.trim()
                            if (!text.isNullOrBlank()) {
                                onSpeechRecognized(text)
                            }
                        }

                        override fun onPartialResults(partialResults: Bundle?) {
                            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            val text = matches?.firstOrNull()?.trim()
                            if (!text.isNullOrBlank()) {
                                onPartialSpeech(text)
                                if (_isPlayingVoice.value) {
                                    stopVoicePlayback()
                                    onInterruptionDetected()
                                }
                            }
                        }

                        override fun onEvent(eventType: Int, params: Bundle?) {}
                    })
                }

                recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1200L)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 800L)
                }
            }
        }
    }

    /**
     * Start PCM16 16kHz streaming and audio processing
     */
    fun startMicStream() {
        if (_isStreaming.value) return
        _isStreaming.value = true

        val bufferSize = AudioRecord.getMinBufferSize(
            SAMPLE_RATE_IN_16K,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        ).coerceAtLeast(2048)

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE_IN_16K,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize * 2
            )
            audioRecord?.startRecording()

            recordingJob = scope.launch(Dispatchers.IO) {
                val shortBuffer = ShortArray(bufferSize / 2)
                while (isActive && _isStreaming.value) {
                    val read = audioRecord?.read(shortBuffer, 0, shortBuffer.size) ?: -1
                    if (read > 0) {
                        // Calculate RMS Energy
                        var sum = 0.0
                        for (i in 0 until read) {
                            sum += (shortBuffer[i] * shortBuffer[i])
                        }
                        val rms = sqrt(sum / read).toFloat()
                        val normalized = (rms / 32767f * 3.5f).coerceIn(0f, 1f)

                        if (!_isPlayingVoice.value) {
                            _amplitude.value = normalized
                        } else {
                            // If assistant is currently speaking and user speaks loud enough, trigger interruption!
                            if (normalized > VAD_INTERRUPTION_THRESHOLD) {
                                scope.launch(Dispatchers.Main) {
                                    stopVoicePlayback()
                                    onInterruptionDetected()
                                }
                            }
                        }
                    }
                }
            }
        } catch (_: SecurityException) {
            // Permission not granted or handled at activity level
        } catch (_: Exception) {}

        startSpeechListening()
    }

    fun stopMicStream() {
        _isStreaming.value = false
        recordingJob?.cancel()
        recordingJob = null
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (_: Exception) {}
        audioRecord = null
        stopSpeechListening()
        _amplitude.value = 0f
    }

    fun startSpeechListening() {
        scope.launch(Dispatchers.Main) {
            if (!isRecognizerListening && speechRecognizer != null && recognizerIntent != null) {
                try {
                    speechRecognizer?.startListening(recognizerIntent)
                } catch (_: Exception) {}
            }
        }
    }

    fun stopSpeechListening() {
        scope.launch(Dispatchers.Main) {
            try {
                speechRecognizer?.stopListening()
            } catch (_: Exception) {}
            isRecognizerListening = false
        }
    }

    /**
     * Play spoken voice output with lively female synthesis
     */
    fun speakText(text: String, utteranceId: String = "mahi_${System.currentTimeMillis()}") {
        if (!isTtsReady || text.isBlank()) {
            onSpeechFinished?.invoke()
            return
        }

        stopVoicePlayback()
        _isPlayingVoice.value = true

        val params = Bundle().apply {
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
        }
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
    }

    /**
     * Supports raw 24kHz PCM audio playback
     */
    fun playPcm24k(pcmData: ByteArray) {
        scope.launch(Dispatchers.IO) {
            stopVoicePlayback()
            _isPlayingVoice.value = true

            try {
                val minBuf = AudioTrack.getMinBufferSize(
                    SAMPLE_RATE_OUT_24K,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                )
                audioTrack = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(SAMPLE_RATE_OUT_24K)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(minBuf.coerceAtLeast(pcmData.size))
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()

                audioTrack?.play()
                startSpeechWaveSimulation()
                audioTrack?.write(pcmData, 0, pcmData.size)
                audioTrack?.stop()
            } catch (_: Exception) {} finally {
                _isPlayingVoice.value = false
                stopSpeechWaveSimulation()
                scope.launch(Dispatchers.Main) {
                    onSpeechFinished?.invoke()
                }
            }
        }
    }

    fun stopVoicePlayback() {
        try {
            tts?.stop()
        } catch (_: Exception) {}
        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (_: Exception) {}
        audioTrack = null
        _isPlayingVoice.value = false
        stopSpeechWaveSimulation()
    }

    private fun startSpeechWaveSimulation() {
        speechSimulationJob?.cancel()
        speechSimulationJob = scope.launch(Dispatchers.Default) {
            var step = 0
            while (isActive && _isPlayingVoice.value) {
                step++
                // Simulated speech rhythm modulation (0.25f - 0.90f)
                val wave = (0.35f + 0.35f * kotlin.math.sin(step * 0.3f).toFloat() +
                        0.2f * kotlin.math.sin(step * 0.7f).toFloat()).coerceIn(0.15f, 0.95f)
                _amplitude.value = wave
                delay(60)
            }
            _amplitude.value = 0f
        }
    }

    private fun stopSpeechWaveSimulation() {
        speechSimulationJob?.cancel()
        speechSimulationJob = null
        _amplitude.value = 0f
    }

    fun release() {
        stopMicStream()
        stopVoicePlayback()
        scope.launch(Dispatchers.Main) {
            speechRecognizer?.destroy()
            speechRecognizer = null
        }
        tts?.shutdown()
        tts = null
    }
}
