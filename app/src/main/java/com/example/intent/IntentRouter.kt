package com.example.intent

import org.json.JSONObject

sealed class ParsedIntent {
    data class DirectTool(val toolName: String, val args: JSONObject) : ParsedIntent()
    object EmergencyStop : ParsedIntent()
    object UserConfirmed : ParsedIntent()
    object UserCancelled : ParsedIntent()
    data class GeneralGemini(val userText: String) : ParsedIntent()
}

object IntentRouter {

    fun parseUserCommand(input: String): ParsedIntent {
        val text = input.trim()
        val lower = text.lowercase()

        // 1. Emergency stop check
        val stopKeywords = listOf("থামো", "থামাও", "চুপ করো", "stop", "halt", "ruko", "band karo")
        if (stopKeywords.any { lower == it || lower.startsWith("$it ") || lower.endsWith(" $it") }) {
            return ParsedIntent.EmergencyStop
        }

        // 2. Confirmation check
        val confirmKeywords = listOf("হ্যাঁ", "হ্যা", "পাঠাও", "করো", "send", "yes", "yep", "sure", "ok", "হুম", "ঠিক আছে", "भेजो", "हाँ")
        if (confirmKeywords.any { lower == it || lower.startsWith("$it ") }) {
            return ParsedIntent.UserConfirmed
        }

        // 3. Cancellation check
        val cancelKeywords = listOf("না", "বাতিল", "করো না", "cancel", "no", "nope", "don't", "dont", "mat bhejo", "नहीं")
        if (cancelKeywords.any { lower == it || lower.startsWith("$it ") }) {
            return ParsedIntent.UserCancelled
        }

        // 4. Quick local matches for rapid offline or instant execution:

        // Battery queries
        if (lower.contains("battery") || lower.contains("ব্যাটারি") || lower.contains("চার্জ") || lower.contains("charging")) {
            return ParsedIntent.DirectTool("getBatteryStatus", JSONObject())
        }

        // Wi-Fi queries
        if (lower.contains("wi-fi") || lower.contains("wifi") || lower.contains("ওয়াইফাই") || lower.contains("ওয়াইফাই")) {
            if (lower.contains("setting") || lower.contains("সেটিংস")) {
                return ParsedIntent.DirectTool("openApp", JSONObject().put("appName", "wifi settings"))
            }
            return ParsedIntent.DirectTool("getWifiStatus", JSONObject())
        }

        // Internet connectivity
        if (lower.contains("internet") || lower.contains("ইন্টারনেট") || lower.contains("নেট আছে")) {
            return ParsedIntent.DirectTool("getNetworkStatus", JSONObject())
        }

        // Storage queries
        if (lower.contains("storage") || lower.contains("স্টোরেজ") || lower.contains("মেমরি") || lower.contains("memory খালি")) {
            return ParsedIntent.DirectTool("getDeviceStatus", JSONObject())
        }

        // Device lock
        if (lower.contains("lock") || lower.contains("লক") || lower.contains("স্ক্রিন অফ") || lower.contains("screen off")) {
            return ParsedIntent.DirectTool("lockDevice", JSONObject())
        }

        // Volume control
        if (lower.contains("volume") || lower.contains("ভলিউম") || lower.contains("আওয়াজ") || lower.contains("সাউন্ড")) {
            return when {
                lower.contains("কমা") || lower.contains("down") || lower.contains("low") || lower.contains("কম") -> {
                    ParsedIntent.DirectTool("volumeControl", JSONObject().put("action", "down"))
                }
                lower.contains("বাড়া") || lower.contains("up") || lower.contains("high") || lower.contains("বেশি") -> {
                    ParsedIntent.DirectTool("volumeControl", JSONObject().put("action", "up"))
                }
                lower.contains("mute") || lower.contains("মিউট") || lower.contains("সাইলেন্ট") -> {
                    ParsedIntent.DirectTool("volumeControl", JSONObject().put("action", "mute"))
                }
                lower.contains("unmute") || lower.contains("আনমিউট") -> {
                    ParsedIntent.DirectTool("volumeControl", JSONObject().put("action", "unmute"))
                }
                else -> ParsedIntent.GeneralGemini(text)
            }
        }

        // Media control
        if (lower.contains("pause") || lower.contains("মিউজিক থামাও") || lower.contains("গান থামাও") || lower.contains("গান বন্ধ")) {
            return ParsedIntent.DirectTool("mediaControl", JSONObject().put("command", "pause"))
        }
        if (lower.contains("next song") || lower.contains("পরের গান") || lower.contains("next track")) {
            return ParsedIntent.DirectTool("mediaControl", JSONObject().put("command", "next"))
        }
        if (lower.contains("previous song") || lower.contains("আগের গান")) {
            return ParsedIntent.DirectTool("mediaControl", JSONObject().put("command", "previous"))
        }
        if (lower.contains("play music") || lower.contains("গান চালাও") || lower.contains("গান বাজা")) {
            return ParsedIntent.DirectTool("mediaControl", JSONObject().put("command", "play"))
        }

        // Camera
        if (lower.contains("camera") || lower.contains("ক্যামেরা") || lower.contains("ছবি তোল") || lower.contains("ক্যামেরা খোলো")) {
            return ParsedIntent.DirectTool("openApp", JSONObject().put("appName", "Camera"))
        }

        // Bluetooth settings
        if (lower.contains("bluetooth") || lower.contains("ব্লুটুথ")) {
            return ParsedIntent.DirectTool("openApp", JSONObject().put("appName", "Bluetooth settings"))
        }

        // Notification reader
        if (lower.contains("notification") || lower.contains("নোটিফিকেশন") || lower.contains("মেসেজ কি এসেছে")) {
            return ParsedIntent.DirectTool("notificationReader", JSONObject().put("limit", 3))
        }

        // App launches: YouTube
        if ((lower.contains("youtube") || lower.contains("ইউটিউব")) &&
            (lower.contains("খোল") || lower.contains("চালু") || lower.contains("open") || lower.contains("play") || lower.contains("start"))) {
            return ParsedIntent.DirectTool("openApp", JSONObject().put("appName", "YouTube"))
        }

        // App launches: WhatsApp
        if ((lower.contains("whatsapp") || lower.contains("হোয়াটসঅ্যাপ") || lower.contains("হোয়াটসঅ্যাপ")) &&
            !lower.contains("message") && !lower.contains("মেসেজ") && !lower.contains("বলো")) {
            return ParsedIntent.DirectTool("openApp", JSONObject().put("appName", "WhatsApp"))
        }

        // WhatsApp message command: e.g. "WhatsApp খুলে Rahim-কে message দাও" or "Rahim-কে বলো আমি আজ আসতে পারব না"
        val messageMatch = detectWhatsAppMessageIntent(lower, text)
        if (messageMatch != null) {
            return ParsedIntent.DirectTool("prepareMessage", messageMatch)
        }

        // Default to Gemini reasoning
        return ParsedIntent.GeneralGemini(text)
    }

    private fun detectWhatsAppMessageIntent(lower: String, original: String): JSONObject? {
        val isMessageIntent = lower.contains("message") || lower.contains("মেসেজ") ||
                lower.contains("বলো") || lower.contains("পাঠাও") || lower.contains("bolo")

        if (!isMessageIntent) return null

        // Try to extract recipient and message
        // Patterns: "Rahim-কে বলো [message]" or "WhatsApp-এ Rahim-কে message দাও [message]"
        val words = original.split(" ")
        var recipient = ""
        var message = ""

        for ((index, word) in words.withIndex()) {
            val clean = word.replace(",", "").replace("-কে", "").replace("কে", "")
            if (word.contains("-কে") || word.contains("কে") && index > 0) {
                recipient = clean
                if (index + 1 < words.size) {
                    message = words.subList(index + 1, words.size).joinToString(" ")
                        .replace("বলো", "")
                        .replace("message দাও", "")
                        .replace("মেসেজ দাও", "")
                        .replace("পাঠাও", "")
                        .trim()
                }
                break
            }
        }

        if (recipient.isNotBlank() && message.isNotBlank()) {
            return JSONObject().apply {
                put("recipient", recipient)
                put("messageText", message)
                put("platform", "whatsapp")
            }
        }

        return null
    }
}
