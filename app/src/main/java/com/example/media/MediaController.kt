package com.example.media

import android.content.Context
import android.media.AudioManager
import android.view.KeyEvent

enum class MediaAction {
    PLAY,
    PAUSE,
    NEXT,
    PREVIOUS,
    TOGGLE,
    STOP
}

enum class VolumeDirection {
    UP,
    DOWN
}

class MediaController(private val context: Context) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

    fun adjustVolume(direction: VolumeDirection): String {
        val am = audioManager ?: return "Audio service is not available."
        val flag = AudioManager.FLAG_SHOW_UI or AudioManager.FLAG_PLAY_SOUND
        val dir = if (direction == VolumeDirection.UP) AudioManager.ADJUST_RAISE else AudioManager.ADJUST_LOWER

        am.adjustStreamVolume(AudioManager.STREAM_MUSIC, dir, flag)
        val current = am.getStreamVolume(AudioManager.STREAM_MUSIC)
        val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val pct = (current * 100) / max
        return if (direction == VolumeDirection.UP) {
            "Volume বাড়ানো হয়েছে ($pct%)।"
        } else {
            "Volume কমানো হয়েছে ($pct%)।"
        }
    }

    fun setVolumePercent(percent: Int): String {
        val am = audioManager ?: return "Audio service is not available."
        val clamped = percent.coerceIn(0, 100)
        val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val target = (clamped * max) / 100
        am.setStreamVolume(AudioManager.STREAM_MUSIC, target, AudioManager.FLAG_SHOW_UI)
        return "Volume $clamped% এ সেট করা হয়েছে।"
    }

    fun setMute(mute: Boolean): String {
        val am = audioManager ?: return "Audio service is not available."
        am.adjustStreamVolume(
            AudioManager.STREAM_MUSIC,
            if (mute) AudioManager.ADJUST_MUTE else AudioManager.ADJUST_UNMUTE,
            AudioManager.FLAG_SHOW_UI
        )
        return if (mute) "ফোন মিউট করা হয়েছে।" else "ফোন আনমিউট করা হয়েছে।"
    }

    fun sendMediaCommand(action: MediaAction): String {
        val am = audioManager ?: return "Audio service is not available."
        val keycode = when (action) {
            MediaAction.PLAY -> KeyEvent.KEYCODE_MEDIA_PLAY
            MediaAction.PAUSE -> KeyEvent.KEYCODE_MEDIA_PAUSE
            MediaAction.NEXT -> KeyEvent.KEYCODE_MEDIA_NEXT
            MediaAction.PREVIOUS -> KeyEvent.KEYCODE_MEDIA_PREVIOUS
            MediaAction.TOGGLE -> KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
            MediaAction.STOP -> KeyEvent.KEYCODE_MEDIA_STOP
        }

        val eventDown = KeyEvent(KeyEvent.ACTION_DOWN, keycode)
        val eventUp = KeyEvent(KeyEvent.ACTION_UP, keycode)
        am.dispatchMediaKeyEvent(eventDown)
        am.dispatchMediaKeyEvent(eventUp)

        return when (action) {
            MediaAction.PLAY -> "Media play করা হচ্ছে।"
            MediaAction.PAUSE -> "Media pause করা হয়েছে।"
            MediaAction.NEXT -> "Next song চালু করা হচ্ছে।"
            MediaAction.PREVIOUS -> "Previous song চালু করা হচ্ছে।"
            MediaAction.TOGGLE -> "Media toggle করা হয়েছে।"
            MediaAction.STOP -> "Media stop করা হয়েছে।"
        }
    }
}
